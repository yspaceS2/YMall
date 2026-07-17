package com.ymall.backend.notification.event;

public interface NotificationEventPublisher {

    void publish(NotificationEvent event);
}
