package com.ymall.backend.order.service;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.ymall.backend.order.config.PendingOrderExpirationProperties;

class PendingOrderExpirationSchedulerTest {

    @Test
    void calculatesExpirationCutoffFromInjectedUtcClock() {
        PendingOrderExpirationService expirationService =
            mock(PendingOrderExpirationService.class);
        PendingOrderExpirationProperties properties =
            new PendingOrderExpirationProperties(Duration.ofMinutes(30), 100);
        Clock clock = Clock.fixed(
            Instant.parse("2026-07-31T15:10:00Z"),
            ZoneOffset.UTC
        );
        PendingOrderExpirationScheduler scheduler = new PendingOrderExpirationScheduler(
            expirationService,
            properties,
            clock
        );

        scheduler.expirePendingOrders();

        then(expirationService).should().expireCreatedOnOrBefore(
            LocalDateTime.of(2026, 7, 31, 14, 40)
        );
    }
}
