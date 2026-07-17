package com.ymall.backend.notification.dto;

import java.time.LocalDateTime;

import com.ymall.backend.notification.entity.Notification;
import com.ymall.backend.notification.entity.NotificationType;

public record NotificationResponse(
    Long notificationId,
    NotificationType type,
    String title,
    String message,
    String targetUrl,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getTargetUrl(),
            notification.getReadAt(),
            notification.getCreatedAt()
        );
    }
}
