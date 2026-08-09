package com.ymall.backend.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.seller.config.SellerOrderPrivacyProperties;

class SellerDeliveryAddressPrivacyPolicyTest {

    private static final Instant NOW = Instant.parse("2026-07-31T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration RETENTION = Duration.ofDays(90);

    private final SellerDeliveryAddressPrivacyPolicy policy =
        new SellerDeliveryAddressPrivacyPolicy(
            CLOCK,
            new SellerOrderPrivacyProperties(RETENTION)
        );

    @Test
    void keepsAddressVisibleBeforeRetentionBoundary() {
        OrderItem item = deliveredItem(LocalDateTime.ofInstant(
            NOW.minus(RETENTION).plusSeconds(1),
            ZoneOffset.UTC
        ));

        assertThat(policy.shouldMask(List.of(item))).isFalse();
    }

    @Test
    void masksAddressAtRetentionBoundary() {
        OrderItem item = deliveredItem(LocalDateTime.ofInstant(
            NOW.minus(RETENTION),
            ZoneOffset.UTC
        ));

        assertThat(policy.shouldMask(List.of(item))).isTrue();
    }

    @Test
    void usesLatestDeliveryCompletionAcrossSellerItems() {
        OrderItem expiredItem = deliveredItem(LocalDateTime.ofInstant(
            NOW.minus(RETENTION),
            ZoneOffset.UTC
        ));
        OrderItem recentItem = deliveredItem(LocalDateTime.ofInstant(
            NOW.minus(Duration.ofDays(10)),
            ZoneOffset.UTC
        ));

        assertThat(policy.shouldMask(List.of(expiredItem, recentItem))).isFalse();
    }

    @Test
    void keepsAddressVisibleWhileAnActiveItemIsUndelivered() {
        OrderItem deliveredItem = deliveredItem(LocalDateTime.ofInstant(
            NOW.minus(RETENTION),
            ZoneOffset.UTC
        ));
        OrderItem pendingItem = mock(OrderItem.class);
        given(pendingItem.getRefundableQuantity()).willReturn(1);
        given(pendingItem.getDeliveredAt()).willReturn(null);

        assertThat(policy.shouldMask(List.of(deliveredItem, pendingItem))).isFalse();
    }

    private OrderItem deliveredItem(LocalDateTime deliveredAt) {
        OrderItem item = mock(OrderItem.class);
        given(item.getRefundableQuantity()).willReturn(1);
        given(item.getDeliveredAt()).willReturn(deliveredAt);
        return item;
    }
}
