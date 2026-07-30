package com.ymall.backend.seller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.seller.entity.SellerApplicationStatus;

public record SellerApplicationReviewRequest(
    @NotNull SellerApplicationStatus status,
    @Size(max = 500) String rejectionReason
) {

    public SellerApplicationReviewRequest {
        rejectionReason = rejectionReason == null ? null : rejectionReason.trim();
    }
}
