package com.ymall.backend.seller.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.seller.config.SellerOrderPrivacyProperties;

@Component
@RequiredArgsConstructor
public class SellerDeliveryAddressPrivacyPolicy {

    private final Clock clock;
    private final SellerOrderPrivacyProperties properties;

    public boolean shouldMask(List<OrderItem> sellerItems) {
        List<OrderItem> activeItems = sellerItems.stream()
            .filter(item -> item.getRefundableQuantity() > 0)
            .toList();
        if (activeItems.isEmpty()
            || activeItems.stream().anyMatch(item -> item.getDeliveredAt() == null)) {
            return false;
        }

        LocalDateTime deliveryCompletedAt = activeItems.stream()
            .map(OrderItem::getDeliveredAt)
            .max(LocalDateTime::compareTo)
            .orElseThrow();
        LocalDateTime retentionEndsAt = deliveryCompletedAt.plus(
            properties.deliveryAddressRetention()
        );
        return !LocalDateTime.now(clock).isBefore(retentionEndsAt);
    }
}
