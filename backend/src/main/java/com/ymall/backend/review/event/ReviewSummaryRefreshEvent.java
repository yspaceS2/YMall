package com.ymall.backend.review.event;

import java.time.Instant;
import java.util.UUID;

public record ReviewSummaryRefreshEvent(
    UUID eventId,
    Long productId,
    Instant occurredAt,
    int schemaVersion
) {

    public static ReviewSummaryRefreshEvent create(Long productId) {
        return new ReviewSummaryRefreshEvent(UUID.randomUUID(), productId, Instant.now(), 1);
    }
}
