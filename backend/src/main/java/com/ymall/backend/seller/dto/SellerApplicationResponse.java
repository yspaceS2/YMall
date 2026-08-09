package com.ymall.backend.seller.dto;

import java.time.LocalDateTime;

import com.ymall.backend.seller.entity.SellerApplicationStatus;

public record SellerApplicationResponse(
    Long sellerApplicationId,
    Long memberId,
    String memberName,
    String memberEmail,
    String storeName,
    String businessNumber,
    String description,
    SellerApplicationStatus status,
    String rejectionReason,
    LocalDateTime reviewedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
