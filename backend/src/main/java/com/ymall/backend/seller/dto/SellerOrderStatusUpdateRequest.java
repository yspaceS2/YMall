package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record SellerOrderStatusUpdateRequest(
    @NotNull OrderItemFulfillmentStatus fulfillmentStatus,
    @Size(max = 50) String carrier,
    @Size(max = 100) String trackingNumber
) {
}
