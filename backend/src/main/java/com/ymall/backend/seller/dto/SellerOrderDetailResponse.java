package com.ymall.backend.seller.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.order.entity.OrderStatus;

public record SellerOrderDetailResponse(
    Long orderId,
    OrderStatus orderStatus,
    BigDecimal sellerAmount,
    LocalDateTime createdAt,
    boolean refundSupported,
    SellerDeliveryAddressResponse deliveryAddress,
    List<SellerOrderItemResponse> items
) {
}
