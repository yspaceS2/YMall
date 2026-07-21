package com.ymall.backend.global.messaging;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ymall.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class DeadLetterByteKafkaOperations implements DisposableBean {

    private final DefaultKafkaProducerFactory<String, byte[]> producerFactory;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public DeadLetterByteKafkaOperations(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = new HashMap<>(
            kafkaProperties.buildProducerProperties()
        );
        this.producerFactory = new DefaultKafkaProducerFactory<>(
            producerProperties,
            new StringSerializer(),
            new ByteArraySerializer()
        );
        this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    public KafkaTemplate<String, byte[]> kafkaTemplate() {
        return kafkaTemplate;
    }

    @Override
    public void destroy() {
        producerFactory.destroy();
    }
}
