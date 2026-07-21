package com.ymall.backend.global.messaging.outbox;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.kafka.outbox")
public record OrderOutboxProperties(
    int batchSize,
    int maxAttempts,
    Duration retryDelay,
    Duration sendTimeout,
    Duration publishedRetention
) {

    public OrderOutboxProperties {
        if (batchSize < 1 || maxAttempts < 1) {
            throw new IllegalArgumentException("Outbox batch size and max attempts must be positive.");
        }
        if (retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()
            || sendTimeout == null || sendTimeout.isNegative() || sendTimeout.isZero()
            || publishedRetention == null || publishedRetention.isNegative()) {
            throw new IllegalArgumentException("Outbox duration settings are invalid.");
        }
    }
}
