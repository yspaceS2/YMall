package com.ymall.backend.seller.dto;

import com.ymall.backend.order.entity.DeliveryAddressSnapshot;

public record SellerDeliveryAddressResponse(
    String recipientName,
    String recipientPhone,
    String postalCode,
    String roadAddress,
    String detailAddress
) {

    public static SellerDeliveryAddressResponse from(DeliveryAddressSnapshot address) {
        if (address == null) {
            return null;
        }
        return new SellerDeliveryAddressResponse(
            address.getRecipientName(),
            address.getRecipientPhone(),
            address.getPostalCode(),
            address.getRoadAddress(),
            address.getDetailAddress()
        );
    }
}
