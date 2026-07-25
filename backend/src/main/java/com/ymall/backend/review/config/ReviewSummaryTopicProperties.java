package com.ymall.backend.review.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.kafka.review-summary")
public record ReviewSummaryTopicProperties(
    String name,
    int partitions,
    short replicationFactor,
    Duration retention,
    String groupId
) {

    public ReviewSummaryTopicProperties {
        if (name == null || name.isBlank() || groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Review summary Kafka settings are required.");
        }
        if (partitions < 1 || replicationFactor < 1 || retention.isNegative()) {
            throw new IllegalArgumentException("Review summary Kafka settings are invalid.");
        }
    }

    public String dltName() {
        return name + ".dlt";
    }
}
