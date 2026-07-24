package com.ymall.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.order.entity.OrderStatus;

public record AdminOrderResponse(
    Long orderId,
    Long memberId,
    String memberEmail,
    String memberName,
    OrderStatus status,
    BigDecimal totalAmount,
    List<AdminOrderItemResponse> items,
    boolean refundSupported,
    LocalDateTime createdAt
) {
}
