package com.ymall.backend.settlement.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ymall.backend.settlement.entity.SettlementLedgerEntry;

@Component
class SettlementAmountCalculator {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    SettlementAmounts calculate(List<SettlementLedgerEntry> entries) {
        return entries.stream().reduce(
            new SettlementAmounts(ZERO, ZERO, ZERO),
            (sum, entry) -> new SettlementAmounts(
                sum.gross().add(entry.getGrossAmount()),
                sum.fee().add(entry.getFeeAmount()),
                sum.settlement().add(entry.getSettlementAmount())
            ),
            SettlementAmounts::add
        );
    }
}

record SettlementAmounts(
    BigDecimal gross,
    BigDecimal fee,
    BigDecimal settlement
) {

    SettlementAmounts add(SettlementAmounts other) {
        return new SettlementAmounts(
            gross.add(other.gross),
            fee.add(other.fee),
            settlement.add(other.settlement)
        );
    }
}
