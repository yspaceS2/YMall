package com.ymall.backend.admin.dto;

import java.time.LocalDateTime;

public record AdminSellerResponse(
    Long sellerProfileId,
    Long memberId,
    String email,
    String memberName,
    String storeName,
    String businessNumber,
    LocalDateTime createdAt
) {
}
