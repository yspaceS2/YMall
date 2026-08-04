package com.ymall.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ymall.backend.seller.entity.SellerApplicationStatus;

public record AdminSellerResponse(
    Long sellerProfileId,
    Long memberId,
    String email,
    String memberName,
    String storeName,
    String businessNumber,
    long productCount,
    long pendingProductCount,
    long orderCount,
    BigDecimal grossSalesAmount,
    long refundedQuantity,
    long pendingReturnCount,
    long pendingSupportCount,
    long pendingSettlementCount,
    SellerApplicationStatus applicationStatus,
    String applicationReviewReason,
    LocalDateTime applicationReviewedAt,
    LocalDateTime createdAt
) {
}
