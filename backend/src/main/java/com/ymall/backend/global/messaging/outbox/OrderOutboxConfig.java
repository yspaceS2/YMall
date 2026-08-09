package com.ymall.backend.global.messaging.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OrderOutboxProperties.class)
@ConditionalOnProperty(name = "ymall.kafka.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OrderOutboxConfig {
}
