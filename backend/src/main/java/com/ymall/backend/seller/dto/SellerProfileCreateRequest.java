package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerProfileCreateRequest(
    @NotBlank @Size(max = 100) String storeName,
    @NotBlank @Size(max = 20) String businessNumber,
    @Size(max = 2000) String description
) {

    public SellerProfileCreateRequest {
        storeName = storeName == null ? null : storeName.trim();
        businessNumber = businessNumber == null ? null : businessNumber.trim();
        description = description == null ? null : description.trim();
    }
}
