package com.ymall.backend.seller.dto;

import java.math.BigDecimal;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record SellerOrderItemResponse(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal lineTotal,
    OrderItemFulfillmentStatus fulfillmentStatus
) {
}
