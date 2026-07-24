package com.ymall.backend.order.dto;

import java.math.BigDecimal;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record OrderItemResponse(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    Integer refundedQuantity,
    BigDecimal totalPrice,
    OrderItemFulfillmentStatus fulfillmentStatus
) {
}
