package com.ymall.backend.global.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties({
    OrderEventTopicProperties.class,
    KafkaConsumerRetryProperties.class
})
@ConditionalOnProperty(name = "ymall.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderEventsTopic(OrderEventTopicProperties properties) {
        return TopicBuilder.name(properties.name())
            .partitions(properties.partitions())
            .replicas(properties.replicationFactor())
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(properties.retention().toMillis()))
            .build();
    }

    @Bean
    public NewTopic orderEventsDltTopic(
        OrderEventTopicProperties topicProperties,
        KafkaConsumerRetryProperties retryProperties
    ) {
        return TopicBuilder.name(topicProperties.dltName())
            .partitions(topicProperties.partitions())
            .replicas(topicProperties.replicationFactor())
            .config(
                TopicConfig.RETENTION_MS_CONFIG,
                String.valueOf(retryProperties.dltRetention().toMillis())
            )
            .build();
    }
}
