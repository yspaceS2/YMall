package com.ymall.backend.member.dto;

public record MemberAddressResponse(
    Long addressId,
    String addressName,
    String recipientName,
    String recipientPhone,
    String postalCode,
    String roadAddress,
    String detailAddress,
    boolean isDefault
) {
}
