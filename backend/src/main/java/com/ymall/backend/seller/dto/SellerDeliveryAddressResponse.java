package com.ymall.backend.seller.dto;

import com.ymall.backend.order.entity.DeliveryAddressSnapshot;

public record SellerDeliveryAddressResponse(
    String recipientName,
    String recipientPhone,
    String postalCode,
    String roadAddress,
    String detailAddress,
    boolean masked
) {

    public static SellerDeliveryAddressResponse from(
        DeliveryAddressSnapshot address,
        boolean masked
    ) {
        if (address == null) {
            return null;
        }
        if (masked) {
            return new SellerDeliveryAddressResponse(
                "***",
                "***",
                "***",
                "***",
                null,
                true
            );
        }
        return new SellerDeliveryAddressResponse(
            address.getRecipientName(),
            address.getRecipientPhone(),
            address.getPostalCode(),
            address.getRoadAddress(),
            address.getDetailAddress(),
            false
        );
    }
}
