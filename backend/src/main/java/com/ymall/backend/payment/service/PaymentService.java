package com.ymall.backend.payment.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.dto.MockPaymentRequest;
import com.ymall.backend.payment.dto.PaymentConfirmRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentConfirmCommand;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.mapper.PaymentMapper;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String MOCK_FAILURE_MESSAGE = "모의 결제에 실패했습니다. 다시 시도해 주세요.";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderOutboxService orderOutboxService;
    private final ProductRepository productRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponse processPayment(
        Long memberId,
        Long orderId,
        MockPaymentRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return paymentRepository.findByOrderIdAndIdempotencyKey(orderId, request.idempotencyKey())
            .map(paymentMapper::toPaymentResponse)
            .orElseGet(() -> processNewPayment(order, request));
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentResponse confirmPayment(
        Long memberId,
        Long orderId,
        PaymentConfirmRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        Payment existingPayment = paymentRepository
            .findByOrderIdAndIdempotencyKey(orderId, request.idempotencyKey())
            .orElse(null);
        if (existingPayment != null) {
            return paymentMapper.toPaymentResponse(existingPayment);
        }

        Payment paymentWithSameKey = paymentRepository.findByPaymentKey(request.paymentKey())
            .orElse(null);
        if (paymentWithSameKey != null) {
            if (paymentWithSameKey.getOrder().getId().equals(orderId)) {
                return paymentMapper.toPaymentResponse(paymentWithSameKey);
            }
            throw new BusinessException(ErrorCode.PAYMENT_KEY_CONFLICT);
        }

        validateConfirmationRequest(order, request);
        ensurePaymentAllowed(order);
        reserveInventoryIfNeeded(order);

        try {
            PaymentGatewayResult gatewayResult = paymentGateway.confirm(new PaymentConfirmCommand(
                request.paymentKey(),
                request.paymentOrderId(),
                request.amount(),
                request.idempotencyKey()
            ));
            validateGatewayResult(request, gatewayResult);
            order.completePayment();

            Payment payment = paymentRepository.save(Payment.success(
                order,
                request.idempotencyKey(),
                gatewayResult.paymentKey(),
                gatewayResult.orderId(),
                request.amount(),
                gatewayResult.totalAmount(),
                gatewayResult.method(),
                gatewayResult.approvedAt()
            ));
            savePaymentEvent(order, PaymentResult.SUCCESS, null);
            return paymentMapper.toPaymentResponse(payment);
        } catch (PaymentException exception) {
            order.failPayment();
            releaseInventory(order);

            Payment payment = paymentRepository.save(Payment.failure(
                order,
                request.idempotencyKey(),
                request.paymentKey(),
                request.paymentOrderId(),
                request.amount(),
                resolveFailureCode(exception),
                exception.getErrorCode().getMessage()
            ));
            savePaymentEvent(order, PaymentResult.FAILURE, payment.getFailureCode());
            throw exception;
        }
    }

    private PaymentResponse processNewPayment(Order order, MockPaymentRequest request) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
            && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        String failureMessage = null;
        if (request.result() == PaymentResult.SUCCESS) {
            order.completePayment();
        } else {
            order.failPayment();
            failureMessage = MOCK_FAILURE_MESSAGE;
        }

        Payment payment = paymentRepository.save(new Payment(
            order,
            request.idempotencyKey(),
            request.result(),
            failureMessage
        ));
        OrderEventType eventType = request.result() == PaymentResult.SUCCESS
            ? OrderEventType.PAYMENT_COMPLETED
            : OrderEventType.PAYMENT_FAILED;
        orderOutboxService.save(
            eventType,
            order.getId(),
            order.getMember().getId(),
            Map.of(
                "status", order.getStatus().name(),
                "paymentResult", request.result().name()
            )
        );
        return paymentMapper.toPaymentResponse(payment);
    }

    private void validateConfirmationRequest(Order order, PaymentConfirmRequest request) {
        if (!order.getPaymentOrderId().equals(request.paymentOrderId())) {
            throw new BusinessException(ErrorCode.PAYMENT_ORDER_MISMATCH);
        }
        if (order.getTotalAmount().compareTo(request.amount()) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void ensurePaymentAllowed(Order order) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
            && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }
    }

    private void validateGatewayResult(
        PaymentConfirmRequest request,
        PaymentGatewayResult gatewayResult
    ) {
        boolean validResult = gatewayResult != null
            && request.paymentKey().equals(gatewayResult.paymentKey())
            && request.paymentOrderId().equals(gatewayResult.orderId())
            && gatewayResult.totalAmount() != null
            && request.amount().compareTo(gatewayResult.totalAmount()) == 0
            && gatewayResult.status() == PaymentGatewayStatus.DONE;
        if (!validResult) {
            throw new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_ERROR,
                "INVALID_CONFIRMATION_RESPONSE",
                "Toss Payments confirmation response did not match the request."
            );
        }
    }

    private void reserveInventoryIfNeeded(Order order) {
        if (order.isInventoryReserved()) {
            return;
        }

        Map<Long, Product> products = loadProductsForUpdate(order);
        for (OrderItem item : order.getItems()) {
            Product product = requireProduct(products, item);
            if (product.getStatus() != ProductStatus.APPROVED) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_ORDERABLE);
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            product.decreaseStock(item.getQuantity());
        }
        order.reserveInventory();
    }

    private void releaseInventory(Order order) {
        if (!order.isInventoryReserved()) {
            return;
        }

        Map<Long, Product> products = loadProductsForUpdate(order);
        for (OrderItem item : order.getItems()) {
            requireProduct(products, item).increaseStock(item.getQuantity());
        }
        order.releaseInventory();
    }

    private Map<Long, Product> loadProductsForUpdate(Order order) {
        List<Long> productIds = order.getItems().stream()
            .map(item -> item.getProduct().getId())
            .sorted()
            .toList();
        return productRepository.findAllByIdForUpdate(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Product requireProduct(Map<Long, Product> products, OrderItem item) {
        Product product = products.get(item.getProduct().getId());
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void savePaymentEvent(
        Order order,
        PaymentResult paymentResult,
        String failureCode
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", order.getStatus().name());
        payload.put("paymentResult", paymentResult.name());
        if (failureCode != null) {
            payload.put("failureCode", failureCode);
        }
        orderOutboxService.save(
            paymentResult == PaymentResult.SUCCESS
                ? OrderEventType.PAYMENT_COMPLETED
                : OrderEventType.PAYMENT_FAILED,
            order.getId(),
            order.getMember().getId(),
            payload
        );
    }

    private String resolveFailureCode(PaymentException exception) {
        return exception.getProviderCode() == null
            ? exception.getErrorCode().name()
            : exception.getProviderCode();
    }
}
