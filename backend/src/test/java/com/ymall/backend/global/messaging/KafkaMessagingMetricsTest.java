package com.ymall.backend.global.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class KafkaMessagingMetricsTest {

    @Test
    void recordsRetryDeadLetterAndOutboxMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KafkaMessagingMetrics metrics = new KafkaMessagingMetrics(registry);

        metrics.recordConsumerRetry();
        metrics.recordDeadLetter("orders");
        metrics.recordOutboxPublished();
        metrics.recordOutboxFailure(false);
        metrics.recordOutboxFailure(true);

        assertThat(registry.get("ymall.kafka.consumer.retries")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("ymall.kafka.consumer.dead.letters")
            .tag("source.topic", "orders")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("ymall.kafka.outbox.published")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("ymall.kafka.outbox.publish.failures")
            .tag("permanent", "false")
            .counter()
            .count()).isEqualTo(1);
        assertThat(registry.get("ymall.kafka.outbox.publish.failures")
            .tag("permanent", "true")
            .counter()
            .count()).isEqualTo(1);
    }
}
