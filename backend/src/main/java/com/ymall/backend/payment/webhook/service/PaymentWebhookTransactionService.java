package com.ymall.backend.payment.webhook.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.service.PaymentInventoryService;
import com.ymall.backend.payment.webhook.dto.TossPaymentWebhookRequest;
import com.ymall.backend.payment.webhook.entity.PaymentWebhookEvent;
import com.ymall.backend.payment.webhook.entity.PaymentWebhookProcessingResult;
import com.ymall.backend.payment.webhook.repository.PaymentWebhookEventRepository;

@Service
@RequiredArgsConstructor
public class PaymentWebhookTransactionService {

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentInventoryService paymentInventoryService;
    private final OrderOutboxService orderOutboxService;

    @Transactional
    public void process(
        String transmissionId,
        TossPaymentWebhookRequest request,
        PaymentGatewayStatus requestedStatus,
        PaymentGatewayResult verifiedPayment
    ) {
        validateVerifiedPayment(request, verifiedPayment);

        Order order = orderRepository.findByPaymentOrderIdForUpdate(verifiedPayment.orderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID));
        if (webhookEventRepository.existsByTransmissionId(transmissionId)) {
            return;
        }
        validateOrder(order, verifiedPayment);

        Payment payment = paymentRepository.findByPaymentKey(verifiedPayment.paymentKey())
            .orElse(null);
        if (payment != null && !payment.getOrder().getId().equals(order.getId())) {
            throw new BusinessException(ErrorCode.PAYMENT_KEY_CONFLICT);
        }

        PaymentWebhookProcessingResult result = applyVerifiedStatus(
            transmissionId,
            order,
            payment,
            verifiedPayment
        );
        if (requestedStatus != verifiedPayment.status()) {
            result = PaymentWebhookProcessingResult.STALE_EVENT;
        }

        webhookEventRepository.save(new PaymentWebhookEvent(
            transmissionId,
            request.eventType(),
            verifiedPayment.paymentKey(),
            verifiedPayment.orderId(),
            requestedStatus,
            verifiedPayment.status(),
            result,
            request.createdAt()
        ));
    }

    private PaymentWebhookProcessingResult applyVerifiedStatus(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        return switch (verifiedPayment.status()) {
            case DONE -> applyCompleted(transmissionId, order, payment, verifiedPayment);
            case ABORTED, EXPIRED -> applyFailed(
                transmissionId,
                order,
                payment,
                verifiedPayment
            );
            case CANCELED -> applyCanceled(transmissionId, order, payment, verifiedPayment);
            case PARTIAL_CANCELED -> applyPartialCancellation(
                transmissionId,
                order,
                payment,
                verifiedPayment
            );
            case READY, IN_PROGRESS, WAITING_FOR_DEPOSIT -> {
                if (payment != null) {
                    payment.synchronizeProviderStatus(verifiedPayment.status());
                }
                yield PaymentWebhookProcessingResult.NO_CHANGE;
            }
            case UNKNOWN -> throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        };
    }

    private PaymentWebhookProcessingResult applyCompleted(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        if (isFulfillmentStarted(order.getStatus()) || order.getStatus() == OrderStatus.CANCELED) {
            synchronizeSuccessfulPayment(transmissionId, order, payment, verifiedPayment);
            return PaymentWebhookProcessingResult.NO_CHANGE;
        }

        boolean statusChanged = order.getStatus() != OrderStatus.PAID;
        if (statusChanged) {
            paymentInventoryService.reserveIfNeeded(order);
            order.completePayment();
        }
        synchronizeSuccessfulPayment(transmissionId, order, payment, verifiedPayment);
        if (statusChanged) {
            saveOrderEvent(order, OrderEventType.PAYMENT_COMPLETED, verifiedPayment.status());
        }
        return statusChanged
            ? PaymentWebhookProcessingResult.APPLIED
            : PaymentWebhookProcessingResult.NO_CHANGE;
    }

    private PaymentWebhookProcessingResult applyFailed(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        boolean statusChanged = !isFulfillmentStarted(order.getStatus())
            && order.getStatus() != OrderStatus.CANCELED
            && order.getStatus() != OrderStatus.PAYMENT_FAILED;
        if (statusChanged) {
            order.failPayment();
            paymentInventoryService.releaseIfReserved(order);
        }

        if (payment == null) {
            payment = Payment.failure(
                order,
                webhookIdempotencyKey(transmissionId),
                verifiedPayment.paymentKey(),
                verifiedPayment.orderId(),
                verifiedPayment.totalAmount(),
                verifiedPayment.status().name(),
                "Payment status was synchronized by a verified webhook."
            );
            payment.synchronizeFailure(verifiedPayment);
            paymentRepository.save(payment);
        } else {
            payment.synchronizeFailure(verifiedPayment);
        }
        if (statusChanged) {
            saveOrderEvent(order, OrderEventType.PAYMENT_FAILED, verifiedPayment.status());
        }
        return statusChanged
            ? PaymentWebhookProcessingResult.APPLIED
            : PaymentWebhookProcessingResult.NO_CHANGE;
    }

    private PaymentWebhookProcessingResult applyCanceled(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        Payment synchronizedPayment = synchronizeSuccessfulPayment(
            transmissionId,
            order,
            payment,
            verifiedPayment
        );
        if (isFulfillmentStarted(order.getStatus()) || order.getStatus() == OrderStatus.CANCELED) {
            return PaymentWebhookProcessingResult.NO_CHANGE;
        }

        paymentInventoryService.releaseIfReserved(order);
        order.cancel();
        synchronizedPayment.synchronizeProviderStatus(verifiedPayment.status());
        saveOrderEvent(order, OrderEventType.ORDER_CANCELED, verifiedPayment.status());
        return PaymentWebhookProcessingResult.APPLIED;
    }

    private PaymentWebhookProcessingResult applyPartialCancellation(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        synchronizeSuccessfulPayment(transmissionId, order, payment, verifiedPayment);
        return PaymentWebhookProcessingResult.NO_CHANGE;
    }

    private Payment synchronizeSuccessfulPayment(
        String transmissionId,
        Order order,
        Payment payment,
        PaymentGatewayResult verifiedPayment
    ) {
        if (payment == null) {
            payment = Payment.success(
                order,
                webhookIdempotencyKey(transmissionId),
                verifiedPayment.paymentKey(),
                verifiedPayment.orderId(),
                verifiedPayment.totalAmount(),
                verifiedPayment.totalAmount(),
                verifiedPayment.method(),
                verifiedPayment.approvedAt()
            );
            paymentRepository.save(payment);
        }
        payment.synchronizeSuccess(verifiedPayment);
        return payment;
    }

    private void validateVerifiedPayment(
        TossPaymentWebhookRequest request,
        PaymentGatewayResult verifiedPayment
    ) {
        boolean valid = verifiedPayment != null
            && request.data().paymentKey().equals(verifiedPayment.paymentKey())
            && request.data().orderId().equals(verifiedPayment.orderId())
            && verifiedPayment.totalAmount() != null
            && request.data().totalAmount().compareTo(verifiedPayment.totalAmount()) == 0
            && verifiedPayment.status() != null
            && verifiedPayment.status() != PaymentGatewayStatus.UNKNOWN;
        if (!valid) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID);
        }
    }

    private void validateOrder(Order order, PaymentGatewayResult verifiedPayment) {
        if (order.getTotalAmount().compareTo(verifiedPayment.totalAmount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private boolean isFulfillmentStarted(OrderStatus status) {
        return status == OrderStatus.PREPARING
            || status == OrderStatus.SHIPPED
            || status == OrderStatus.DELIVERED;
    }

    private void saveOrderEvent(
        Order order,
        OrderEventType eventType,
        PaymentGatewayStatus providerStatus
    ) {
        orderOutboxService.save(
            eventType,
            order.getId(),
            order.getMember().getId(),
            Map.of(
                "status", order.getStatus().name(),
                "providerStatus", providerStatus.name()
            )
        );
    }

    private String webhookIdempotencyKey(String transmissionId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(transmissionId.getBytes(StandardCharsets.UTF_8));
            return "webhook:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
