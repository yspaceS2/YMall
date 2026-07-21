package com.ymall.backend.global.messaging;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ymall.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEventEnvelope> kafkaTemplate;
    private final OrderEventTopicProperties topicProperties;

    public OrderEventProducer(
        KafkaTemplate<String, OrderEventEnvelope> kafkaTemplate,
        OrderEventTopicProperties topicProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    public CompletableFuture<SendResult<String, OrderEventEnvelope>> send(OrderEventEnvelope event) {
        return kafkaTemplate.send(topicProperties.name(), event.orderId().toString(), event);
    }
}
