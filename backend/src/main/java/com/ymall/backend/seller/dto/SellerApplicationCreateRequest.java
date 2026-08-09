package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerApplicationCreateRequest(
    @NotBlank @Size(max = 100) String storeName,
    @NotBlank
    @Pattern(regexp = "\\d{3}-?\\d{2}-?\\d{5}")
    @Size(max = 20)
    String businessNumber,
    @Size(max = 2000) String description
) {

    public SellerApplicationCreateRequest {
        storeName = storeName == null ? null : storeName.trim();
        businessNumber = businessNumber == null ? null : businessNumber.trim();
        description = description == null ? null : description.trim();
    }
}
