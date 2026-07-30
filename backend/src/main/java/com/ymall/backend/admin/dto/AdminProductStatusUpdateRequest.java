package com.ymall.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.product.entity.ProductStatus;

public record AdminProductStatusUpdateRequest(
    @NotNull ProductStatus status,
    @Size(max = 500) String rejectionReason
) {

    public AdminProductStatusUpdateRequest {
        rejectionReason = rejectionReason == null ? null : rejectionReason.trim();
    }
}
