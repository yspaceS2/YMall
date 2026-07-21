package com.ymall.backend.global.messaging;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.kafka.order-events")
public record OrderEventTopicProperties(
    String name,
    int partitions,
    short replicationFactor,
    Duration retention
) {

    public OrderEventTopicProperties {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Order event topic name must not be blank.");
        }
        if (partitions < 1 || replicationFactor < 1 || retention == null || retention.isNegative()) {
            throw new IllegalArgumentException("Order event topic settings are invalid.");
        }
    }

    public String dltName() {
        return name + ".DLT";
    }
}
