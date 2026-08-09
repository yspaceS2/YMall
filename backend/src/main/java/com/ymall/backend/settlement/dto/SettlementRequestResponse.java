package com.ymall.backend.settlement.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.ymall.backend.settlement.entity.SettlementRequestStatus;

public record SettlementRequestResponse(
    Long settlementRequestId,
    Long sellerProfileId,
    String storeName,
    LocalDate periodStart,
    LocalDate periodEnd,
    SettlementRequestStatus status,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    BigDecimal settlementAmount,
    String rejectionReason,
    String mockPaymentReference,
    Instant reviewedAt,
    Instant paidAt,
    Instant createdAt,
    Instant updatedAt
) {
}
