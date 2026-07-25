package com.ymall.backend.global.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class KafkaRetryMetricsBackOffHandlerTest {

    @Test
    void recordsRetryOnlyWhenNextBackOffIsPerformed() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        KafkaMessagingMetrics metrics = new KafkaMessagingMetrics(registry);
        BackOffHandler noOpDelegate = new BackOffHandler() {
            @Override
            public void onNextBackOff(
                MessageListenerContainer container,
                Exception exception,
                long nextBackOff
            ) {
            }
        };
        KafkaRetryMetricsBackOffHandler handler = new KafkaRetryMetricsBackOffHandler(
            metrics,
            noOpDelegate
        );

        assertThat(registry.find("ymall.kafka.consumer.retries").counter()).isNull();

        handler.onNextBackOff(null, new IllegalStateException("retry"), 100L);

        assertThat(registry.get("ymall.kafka.consumer.retries")
            .counter()
            .count()).isEqualTo(1);
    }
}
