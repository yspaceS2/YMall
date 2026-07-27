package com.ymall.backend.settlement.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.settlement")
public record SettlementProperties(BigDecimal feeRate) {

    public SettlementProperties {
        if (feeRate == null
            || feeRate.signum() < 0
            || feeRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Settlement fee rate must be between 0 and 1.");
        }
    }
}
