package com.ymall.backend.settlement.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.ymall.backend.settlement.entity.SettlementEntryType;
import com.ymall.backend.settlement.entity.SettlementStatus;

public record SettlementLedgerResponse(
    Long settlementLedgerId,
    Long orderId,
    Long orderItemId,
    SettlementEntryType entryType,
    SettlementStatus status,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    BigDecimal settlementAmount,
    Instant occurredAt
) {
}
