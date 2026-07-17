package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerProfileUpdateRequest(
    @NotBlank @Size(max = 100) String storeName,
    @Size(max = 2000) String description
) {

    public SellerProfileUpdateRequest {
        storeName = storeName == null ? null : storeName.trim();
        description = description == null ? null : description.trim();
    }
}
