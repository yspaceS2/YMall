package com.ymall.backend.seller.dto;

import java.time.Instant;

import com.ymall.backend.seller.entity.SettlementAccountVerificationStatus;

public record SellerSettlementAccountResponse(
    Long settlementAccountId,
    String bankCode,
    String bankName,
    String accountHolder,
    String maskedAccountNumber,
    SettlementAccountVerificationStatus verificationStatus,
    Instant verifiedAt,
    Instant updatedAt
) {
}
