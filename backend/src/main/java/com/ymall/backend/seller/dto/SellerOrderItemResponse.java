package com.ymall.backend.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record SellerOrderItemResponse(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    Integer refundedQuantity,
    BigDecimal lineTotal,
    String thumbnailUrl,
    OrderItemFulfillmentStatus fulfillmentStatus,
    String carrier,
    String trackingNumber,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt
) {
}
