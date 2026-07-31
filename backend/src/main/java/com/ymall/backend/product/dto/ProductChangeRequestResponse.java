package com.ymall.backend.product.dto;

import java.time.LocalDateTime;

import com.ymall.backend.product.entity.ProductStatus;

public record ProductChangeRequestResponse(
    Long productChangeRequestId,
    Long productId,
    Long sellerProfileId,
    String storeName,
    ProductStatus status,
    ProductSnapshotResponse current,
    ProductSnapshotResponse proposed,
    String rejectionReason,
    LocalDateTime createdAt,
    LocalDateTime reviewedAt
) {
}
