package com.ymall.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberAddressRequest(
    @NotBlank @Size(max = 30) String addressName,
    @NotBlank @Size(max = 50) String recipientName,
    @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$") String recipientPhone,
    @NotBlank @Pattern(regexp = "^\\d{5}$") String postalCode,
    @NotBlank @Size(max = 255) String roadAddress,
    @NotBlank @Size(max = 255) String detailAddress,
    boolean isDefault
) {
    public MemberAddressRequest {
        addressName = trim(addressName);
        recipientName = trim(recipientName);
        recipientPhone = recipientPhone == null ? null : recipientPhone.replaceAll("[\\s-]", "");
        postalCode = trim(postalCode);
        roadAddress = trim(roadAddress);
        detailAddress = trim(detailAddress);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
