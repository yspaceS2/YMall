package com.ymall.backend.order.dto;

public record OrderDeliveryAddressResponse(
    String recipientName,
    String recipientPhone,
    String postalCode,
    String roadAddress,
    String detailAddress
) {
}
