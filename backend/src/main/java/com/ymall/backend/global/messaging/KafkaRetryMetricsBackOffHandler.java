package com.ymall.backend.global.messaging;

import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.listener.DefaultBackOffHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

final class KafkaRetryMetricsBackOffHandler implements BackOffHandler {

    private final KafkaMessagingMetrics metrics;
    private final BackOffHandler delegate;

    KafkaRetryMetricsBackOffHandler(KafkaMessagingMetrics metrics) {
        this(metrics, new DefaultBackOffHandler());
    }

    KafkaRetryMetricsBackOffHandler(
        KafkaMessagingMetrics metrics,
        BackOffHandler delegate
    ) {
        this.metrics = metrics;
        this.delegate = delegate;
    }

    @Override
    public void onNextBackOff(
        MessageListenerContainer container,
        Exception exception,
        long nextBackOff
    ) {
        metrics.recordConsumerRetry();
        delegate.onNextBackOff(container, exception, nextBackOff);
    }
}
