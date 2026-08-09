package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemRequest;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;

@Component
class PaymentRefundCalculator {

    void validateOrderStatus(Order order, boolean deliveredReturn) {
        boolean returnApproval = deliveredReturn && order.getStatus() == OrderStatus.DELIVERED;
        if (!returnApproval
            && order.getStatus() != OrderStatus.PAID
            && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED
            && order.getStatus() != OrderStatus.PREPARING) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }
    }

    Map<Long, Integer> pendingQuantities(List<PaymentRefund> refunds) {
        Map<Long, Integer> quantities = new HashMap<>();
        refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN)
            .flatMap(refund -> refund.getItems().stream())
            .forEach(item -> quantities.merge(
                item.getOrderItem().getId(),
                item.getQuantity(),
                Integer::sum
            ));
        return quantities;
    }

    List<RefundQuantity> resolveQuantities(
        Order order,
        List<PaymentRefundItemRequest> requests,
        Map<Long, Integer> pendingQuantities,
        Predicate<OrderItem> itemAccess,
        boolean deliveredReturn
    ) {
        Map<Long, OrderItem> items = order.getItems().stream()
            .collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        if (requests == null || requests.isEmpty()) {
            List<RefundQuantity> remaining = order.getItems().stream()
                .filter(itemAccess)
                .map(item -> new RefundQuantity(
                    item,
                    item.getRefundableQuantity()
                        - pendingQuantities.getOrDefault(item.getId(), 0)
                ))
                .filter(quantity -> quantity.quantity() > 0)
                .toList();
            if (remaining.isEmpty()) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
            }
            return remaining;
        }

        Set<Long> uniqueItemIds = new HashSet<>();
        return requests.stream().map(request -> {
            if (!uniqueItemIds.add(request.orderItemId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            OrderItem item = items.get(request.orderItemId());
            int available = item == null
                ? 0
                : item.getRefundableQuantity()
                    - pendingQuantities.getOrDefault(item.getId(), 0);
            if (item == null
                || !itemAccess.test(item)
                || !isRefundableFulfillmentStatus(item, deliveredReturn)
                || request.quantity() > available) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
            }
            return new RefundQuantity(item, request.quantity());
        }).toList();
    }

    BigDecimal remainingAmount(Order order, List<PaymentRefund> refunds) {
        BigDecimal refunded = refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.SUCCEEDED)
            .map(PaymentRefund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = refunds.stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN)
            .map(PaymentRefund::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return order.getTotalAmount().subtract(refunded).subtract(pending);
    }

    BigDecimal calculateRefundAmount(
        RefundQuantity quantity,
        Map<Long, Integer> pendingQuantities
    ) {
        OrderItem item = quantity.item();
        BigDecimal amount = item.getUnitPrice()
            .multiply(BigDecimal.valueOf(quantity.quantity()));
        int availableQuantity = item.getRefundableQuantity()
            - pendingQuantities.getOrDefault(item.getId(), 0);
        return quantity.quantity() == availableQuantity
            ? amount.add(item.getShippingFee())
            : amount;
    }

    boolean isOwnedBySeller(OrderItem item, Long sellerProfileId) {
        return item.getProduct().getSellerProfile() != null
            && item.getProduct().getSellerProfile().getId().equals(sellerProfileId);
    }

    private boolean isRefundableFulfillmentStatus(
        OrderItem item,
        boolean deliveredReturn
    ) {
        OrderItemFulfillmentStatus status = item.getEffectiveFulfillmentStatus();
        return status == OrderItemFulfillmentStatus.PENDING
            || status == OrderItemFulfillmentStatus.PREPARING
            || (deliveredReturn && status == OrderItemFulfillmentStatus.DELIVERED);
    }

    record RefundQuantity(OrderItem item, int quantity) {
    }
}
