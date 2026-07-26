package com.ymall.backend.order.service;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.ymall.backend.order.config.PendingOrderExpirationProperties;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "ymall.order.pending-expiration.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PendingOrderExpirationScheduler {

    private final PendingOrderExpirationService expirationService;
    private final PendingOrderExpirationProperties properties;

    @Scheduled(fixedDelayString = "${ymall.order.pending-expiration.poll-interval:1m}")
    public void expirePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minus(properties.timeout());
        int expiredCount = expirationService.expireCreatedOnOrBefore(cutoff);
        if (expiredCount > 0) {
            log.info("Expired pending orders and restored inventory. count={}", expiredCount);
        }
    }
}
