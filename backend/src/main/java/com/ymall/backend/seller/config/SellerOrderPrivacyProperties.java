package com.ymall.backend.seller.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.seller.order-privacy")
public record SellerOrderPrivacyProperties(
    Duration deliveryAddressRetention
) {

    private static final Duration DEFAULT_DELIVERY_ADDRESS_RETENTION = Duration.ofDays(90);

    public SellerOrderPrivacyProperties {
        deliveryAddressRetention = deliveryAddressRetention == null
            ? DEFAULT_DELIVERY_ADDRESS_RETENTION
            : deliveryAddressRetention;
        if (deliveryAddressRetention.isNegative() || deliveryAddressRetention.isZero()) {
            throw new IllegalArgumentException(
                "Seller delivery address retention must be positive."
            );
        }
    }
}
