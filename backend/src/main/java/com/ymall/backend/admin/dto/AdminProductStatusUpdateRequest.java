package com.ymall.backend.admin.dto;

import jakarta.validation.constraints.NotNull;

import com.ymall.backend.product.entity.ProductStatus;

public record AdminProductStatusUpdateRequest(
    @NotNull ProductStatus status
) {
}
