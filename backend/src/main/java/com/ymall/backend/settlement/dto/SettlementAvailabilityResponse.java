package com.ymall.backend.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementAvailabilityResponse(
    LocalDate periodStart,
    LocalDate periodEnd,
    int entryCount,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    BigDecimal settlementAmount,
    boolean hasSettlementAccount,
    boolean canRequest
) {
}
