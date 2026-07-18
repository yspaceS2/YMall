package com.ymall.backend.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.order.entity.OrderStatus;

public record OrderResponse(
    Long orderId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items,
    OrderDeliveryAddressResponse deliveryAddress,
    LocalDateTime createdAt
) {
    public OrderResponse(Long orderId, OrderStatus status, BigDecimal totalAmount,
        List<OrderItemResponse> items, LocalDateTime createdAt) {
        this(orderId, status, totalAmount, items, null, createdAt);
    }
}
