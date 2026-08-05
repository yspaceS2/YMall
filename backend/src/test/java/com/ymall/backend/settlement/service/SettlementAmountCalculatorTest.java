package com.ymall.backend.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ymall.backend.settlement.entity.SettlementLedgerEntry;

class SettlementAmountCalculatorTest {

    private final SettlementAmountCalculator calculator = new SettlementAmountCalculator();

    @Test
    void calculateSumsGrossFeeAndSettlementAmounts() {
        SettlementLedgerEntry first = entry("10000.00", "300.00", "9700.00");
        SettlementLedgerEntry second = entry("5000.00", "150.00", "4850.00");

        SettlementAmounts amounts = calculator.calculate(List.of(first, second));

        assertThat(amounts.gross()).isEqualByComparingTo("15000.00");
        assertThat(amounts.fee()).isEqualByComparingTo("450.00");
        assertThat(amounts.settlement()).isEqualByComparingTo("14550.00");
    }

    @Test
    void calculateReturnsZeroAmountsForEmptyEntries() {
        SettlementAmounts amounts = calculator.calculate(List.of());

        assertThat(amounts.gross()).isEqualByComparingTo("0.00");
        assertThat(amounts.fee()).isEqualByComparingTo("0.00");
        assertThat(amounts.settlement()).isEqualByComparingTo("0.00");
    }

    private SettlementLedgerEntry entry(
        String gross,
        String fee,
        String settlement
    ) {
        SettlementLedgerEntry entry = mock(SettlementLedgerEntry.class);
        given(entry.getGrossAmount()).willReturn(new BigDecimal(gross));
        given(entry.getFeeAmount()).willReturn(new BigDecimal(fee));
        given(entry.getSettlementAmount()).willReturn(new BigDecimal(settlement));
        return entry;
    }
}
