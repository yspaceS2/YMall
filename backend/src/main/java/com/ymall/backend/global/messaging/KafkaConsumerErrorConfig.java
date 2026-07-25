package com.ymall.backend.global.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BackOffHandler;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.review.config.ReviewSummaryTopicProperties;

@Configuration
@Slf4j
@EnableConfigurationProperties(KafkaConsumerRetryProperties.class)
@ConditionalOnProperty(name = "ymall.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerErrorConfig {

    @Bean
    public DefaultErrorHandler orderEventErrorHandler(
        KafkaTemplate<String, OrderEventEnvelope> kafkaTemplate,
        DeadLetterByteKafkaOperations deadLetterByteKafkaOperations,
        OrderEventTopicProperties topicProperties,
        ReviewSummaryTopicProperties reviewSummaryTopicProperties,
        KafkaConsumerRetryProperties retryProperties,
        KafkaMessagingMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            producerRecord -> producerRecord.value() instanceof byte[]
                ? deadLetterByteKafkaOperations.kafkaTemplate()
                : kafkaTemplate,
            (record, exception) -> new TopicPartition(
                record.topic().equals(reviewSummaryTopicProperties.name())
                    ? reviewSummaryTopicProperties.dltName()
                    : topicProperties.dltName(),
                record.partition()
            )
        );
        ConsumerRecordRecoverer meteredRecoverer = (record, exception) -> {
            recoverer.accept(record, exception);
            metrics.recordDeadLetter(record.topic());
        };
        BackOffHandler meteredBackOffHandler = new KafkaRetryMetricsBackOffHandler(metrics);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            meteredRecoverer,
            new FixedBackOff(
                retryProperties.retryDelay().toMillis(),
                retryProperties.maxRetries()
            ),
            meteredBackOffHandler
        );
        errorHandler.addNotRetryableExceptions(
            BusinessException.class,
            IllegalArgumentException.class,
            DeserializationException.class
        );
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> {
            log.warn(
                "Kafka event consumption failed: topic={}, partition={}, offset={}, attempt={}, error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                exception.getClass().getSimpleName()
            );
        });
        return errorHandler;
    }
}
