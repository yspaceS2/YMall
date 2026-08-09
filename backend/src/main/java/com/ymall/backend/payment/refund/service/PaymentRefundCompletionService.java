package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;

@Service
@RequiredArgsConstructor
class PaymentRefundCompletionService {

    private final OrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;
    private final ProductRepository productRepository;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final OrderOutboxService orderOutboxService;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final PaymentRefundResponseMapper responseMapper;

    @Transactional
    PaymentRefundResponse complete(Long refundId, PaymentGatewayResult gatewayResult) {
        PaymentRefund refund = refundRepository.findByIdForUpdate(refundId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND));
        if (refund.getStatus() != PaymentRefundStatus.PENDING) {
            return responseMapper.toResponse(refund);
        }
        Order order = orderRepository.findByIdForUpdate(refund.getOrder().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        validateGatewayResult(refund, order, gatewayResult);
        return completeConfirmedRefund(refund, order, gatewayResult);
    }

    @Transactional
    boolean reconcile(Long refundId, PaymentGatewayResult gatewayResult) {
        PaymentRefund refund = refundRepository.findByIdForUpdate(refundId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND));
        if (refund.getStatus() != PaymentRefundStatus.UNKNOWN) {
            return true;
        }
        Order order = orderRepository.findByIdForUpdate(refund.getOrder().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!hasMatchingPaymentIdentity(refund, order, gatewayResult)) {
            return false;
        }

        BigDecimal balanceBeforeRefund = expectedBalanceBefore(refund, order);
        BigDecimal balanceAfterRefund = balanceBeforeRefund.subtract(refund.getAmount());
        boolean cancellationApplied = isCancellationStatus(gatewayResult.status())
            && balanceAfterRefund.compareTo(gatewayResult.balanceAmount()) == 0;
        if (cancellationApplied) {
            completeConfirmedRefund(refund, order, gatewayResult);
            return true;
        }

        boolean cancellationNotApplied =
            (gatewayResult.status() == PaymentGatewayStatus.DONE
                || gatewayResult.status() == PaymentGatewayStatus.PARTIAL_CANCELED)
                && balanceBeforeRefund.compareTo(gatewayResult.balanceAmount()) == 0;
        if (cancellationNotApplied) {
            refund.resolveUnknownAsFailed(
                "REFUND_NOT_APPLIED",
                "The payment provider confirmed that the refund was not applied."
            );
            return true;
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordFailure(
        Long refundId,
        String failureCode,
        String failureMessage,
        boolean outcomeUnknown
    ) {
        refundRepository.findByIdForUpdate(refundId)
            .ifPresent(refund -> refund.fail(
                failureCode,
                truncate(failureMessage),
                outcomeUnknown
            ));
    }

    private PaymentRefundResponse completeConfirmedRefund(
        PaymentRefund refund,
        Order order,
        PaymentGatewayResult gatewayResult
    ) {
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(
            refund.getItems().stream()
                .map(item -> item.getOrderItem().getProduct().getId())
                .sorted()
                .toList()
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (PaymentRefundItem refundItem : refund.getItems()) {
            OrderItem orderItem = refundItem.getOrderItem();
            Product product = products.get(orderItem.getProduct().getId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            orderItem.recordRefund(refundItem.getQuantity());
            product.increaseStock(refundItem.getQuantity());
        }
        productCacheInvalidator.evictProductDetails(products.keySet());

        boolean fullyRefunded = gatewayResult.balanceAmount().signum() == 0;
        order.applyRefund(fullyRefunded);
        refund.getPayment().synchronizeProviderStatus(gatewayResult.status());
        refund.succeed(gatewayResult.status(), gatewayResult.balanceAmount());
        orderOutboxService.save(
            OrderEventType.REFUND_COMPLETED,
            order.getId(),
            order.getMember().getId(),
            Map.of(
                "refundId", refund.getId(),
                "refundType", refund.getType().name(),
                "refundAmount", refund.getAmount()
            )
        );
        dashboardRealtimePublisher.invalidateOrder(order.getId());
        return responseMapper.toResponse(refund);
    }

    private void validateGatewayResult(
        PaymentRefund refund,
        Order order,
        PaymentGatewayResult result
    ) {
        BigDecimal expectedBalance = expectedBalanceBefore(refund, order)
            .subtract(refund.getAmount());
        boolean valid = hasMatchingPaymentIdentity(refund, order, result)
            && expectedBalance.compareTo(result.balanceAmount()) == 0
            && isCancellationStatus(result.status());
        if (!valid) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_PROVIDER_MISMATCH);
        }
    }

    private BigDecimal expectedBalanceBefore(PaymentRefund refund, Order order) {
        BigDecimal previouslyRefundedAmount = refundRepository
            .findAllByOrderIdOrderByCreatedAtDesc(order.getId())
            .stream()
            .filter(previousRefund ->
                !previousRefund.getId().equals(refund.getId())
                    && previousRefund.getStatus() == PaymentRefundStatus.SUCCEEDED
            )
            .map(PaymentRefund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return order.getTotalAmount().subtract(previouslyRefundedAmount);
    }

    private boolean hasMatchingPaymentIdentity(
        PaymentRefund refund,
        Order order,
        PaymentGatewayResult result
    ) {
        return result != null
            && refund.getPayment().getPaymentKey().equals(result.paymentKey())
            && order.getPaymentOrderId().equals(result.orderId())
            && result.totalAmount() != null
            && order.getTotalAmount().compareTo(result.totalAmount()) == 0
            && result.balanceAmount() != null;
    }

    private boolean isCancellationStatus(PaymentGatewayStatus status) {
        return status == PaymentGatewayStatus.CANCELED
            || status == PaymentGatewayStatus.PARTIAL_CANCELED;
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
