package com.ymall.backend.notification.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringNotificationEventPublisher implements NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(NotificationEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
