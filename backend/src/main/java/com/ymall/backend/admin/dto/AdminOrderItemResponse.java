package com.ymall.backend.admin.dto;

import java.math.BigDecimal;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record AdminOrderItemResponse(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal,
    OrderItemFulfillmentStatus fulfillmentStatus
) {
}
