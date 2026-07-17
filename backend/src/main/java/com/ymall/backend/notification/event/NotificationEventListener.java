package com.ymall.backend.notification.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ymall.backend.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void handle(NotificationEvent event) {
        notificationService.create(event);
    }
}
