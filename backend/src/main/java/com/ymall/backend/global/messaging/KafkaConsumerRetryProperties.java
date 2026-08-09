package com.ymall.backend.global.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.kafka.consumer-retry")
public record KafkaConsumerRetryProperties(
    int maxRetries,
    Duration retryDelay,
    Duration dltRetention
) {

    public KafkaConsumerRetryProperties {
        if (maxRetries < 0
            || retryDelay == null || retryDelay.isNegative()
            || dltRetention == null || dltRetention.isNegative() || dltRetention.isZero()) {
            throw new IllegalArgumentException("Kafka consumer retry settings are invalid.");
        }
    }
}
