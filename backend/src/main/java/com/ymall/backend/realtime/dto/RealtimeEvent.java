package com.ymall.backend.realtime.dto;

import java.time.LocalDateTime;

public record RealtimeEvent(
    String type,
    String resource,
    Long resourceId,
    LocalDateTime occurredAt
) {

    public static RealtimeEvent of(String type, String resource, Long resourceId) {
        return new RealtimeEvent(type, resource, resourceId, LocalDateTime.now());
    }
}
