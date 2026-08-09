package com.ymall.backend.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.order.entity.OrderStatus;

public record OrderResponse(
    Long orderId,
    String paymentOrderId,
    OrderStatus status,
    BigDecimal totalAmount,
    BigDecimal productAmount,
    BigDecimal shippingFee,
    List<OrderItemResponse> items,
    OrderDeliveryAddressResponse deliveryAddress,
    boolean refundSupported,
    LocalDateTime createdAt
) {
    public OrderResponse(Long orderId, OrderStatus status, BigDecimal totalAmount,
        List<OrderItemResponse> items, LocalDateTime createdAt) {
        this(
            orderId,
            null,
            status,
            totalAmount,
            totalAmount,
            BigDecimal.ZERO,
            items,
            null,
            false,
            createdAt
        );
    }
}
