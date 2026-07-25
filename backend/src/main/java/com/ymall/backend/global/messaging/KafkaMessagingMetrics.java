package com.ymall.backend.global.messaging;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class KafkaMessagingMetrics {

    private static final String UNKNOWN_TOPIC = "unknown";

    private final MeterRegistry meterRegistry;

    public KafkaMessagingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordConsumerRetry() {
        meterRegistry.counter("ymall.kafka.consumer.retries").increment();
    }

    public void recordDeadLetter(String topic) {
        meterRegistry.counter(
            "ymall.kafka.consumer.dead.letters",
            "source.topic",
            normalizedTopic(topic)
        ).increment();
    }

    public void recordOutboxPublished() {
        meterRegistry.counter("ymall.kafka.outbox.published").increment();
    }

    public void recordOutboxFailure(boolean permanentlyFailed) {
        meterRegistry.counter(
            "ymall.kafka.outbox.publish.failures",
            "permanent",
            Boolean.toString(permanentlyFailed)
        ).increment();
    }

    private String normalizedTopic(String topic) {
        return topic == null || topic.isBlank() ? UNKNOWN_TOPIC : topic;
    }
}
