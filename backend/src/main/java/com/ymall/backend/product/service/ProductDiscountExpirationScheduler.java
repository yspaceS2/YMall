package com.ymall.backend.product.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.ProductRepository;

@Component
@RequiredArgsConstructor
public class ProductDiscountExpirationScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private final ProductRepository productRepository;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final Clock clock;

    @Scheduled(
        cron = "${ymall.product.discount-expiration.cron:0 0 0 * * *}",
        zone = "Asia/Seoul"
    )
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void expireDiscounts() {
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        List<Product> expiredProducts =
            productRepository.findByDiscountPercentageGreaterThanAndDiscountEndDateBefore(
                BigDecimal.ZERO,
                today
            );

        expiredProducts.forEach(product -> {
            product.expireDiscount();
            productCacheInvalidator.evictDetail(product.getId());
        });
    }
}
