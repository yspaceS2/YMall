package com.ymall.backend.global.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import com.ymall.backend.global.exception.BusinessException;

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
        KafkaConsumerRetryProperties retryProperties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            producerRecord -> producerRecord.value() instanceof byte[]
                ? deadLetterByteKafkaOperations.kafkaTemplate()
                : kafkaTemplate,
            (record, exception) -> new TopicPartition(
                topicProperties.dltName(), record.partition()
            )
        );
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(
                retryProperties.retryDelay().toMillis(),
                retryProperties.maxRetries()
            )
        );
        errorHandler.addNotRetryableExceptions(
            BusinessException.class,
            IllegalArgumentException.class,
            DeserializationException.class
        );
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> log.warn(
            "Order event consumption failed: topic={}, partition={}, offset={}, attempt={}",
            record.topic(),
            record.partition(),
            record.offset(),
            deliveryAttempt,
            exception
        ));
        return errorHandler;
    }
}
