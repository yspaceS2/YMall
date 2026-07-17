package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotNull;

import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public record SellerOrderStatusUpdateRequest(
    @NotNull OrderItemFulfillmentStatus fulfillmentStatus
) {
}
