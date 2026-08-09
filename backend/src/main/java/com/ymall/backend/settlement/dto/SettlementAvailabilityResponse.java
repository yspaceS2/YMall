package com.ymall.backend.settlement.dto;

import java.math.BigDecimal;

public record SettlementAvailabilityResponse(
    int entryCount,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    BigDecimal settlementAmount,
    boolean hasSettlementAccount,
    boolean canRequest
) {
}
