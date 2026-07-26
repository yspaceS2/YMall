package com.ymall.backend.order.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.order.pending-expiration")
public record PendingOrderExpirationProperties(
    Duration timeout,
    int batchSize
) {

    public PendingOrderExpirationProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Pending order expiration timeout must be positive.");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Pending order expiration batch size must be positive.");
        }
    }
}
