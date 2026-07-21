package com.ymall.backend.notification.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"ymall.kafka.enabled", "ymall.kafka.notification-consumer.enabled"},
    havingValue = "true"
)
public class OrderNotificationConsumer {

    private final OrderNotificationEventMapper eventMapper;
    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${ymall.kafka.order-events.name}",
        groupId = "${ymall.kafka.notification-consumer.group-id}"
    )
    public void consume(OrderEventEnvelope event) {
        notificationService.create(eventMapper.map(event));
    }
}
