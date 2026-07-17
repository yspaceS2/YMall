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
    LocalDateTime createdAt
) {
}
