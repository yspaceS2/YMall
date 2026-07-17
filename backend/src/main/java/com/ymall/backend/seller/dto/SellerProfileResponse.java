package com.ymall.backend.seller.dto;

import java.time.LocalDateTime;

public record SellerProfileResponse(
    Long sellerProfileId,
    Long memberId,
    String storeName,
    String businessNumber,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
