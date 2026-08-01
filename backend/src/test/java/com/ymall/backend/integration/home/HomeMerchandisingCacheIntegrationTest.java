package com.ymall.backend.integration.home;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.home.config.HomeCacheNames;
import com.ymall.backend.home.dto.HomeMerchandisingResponse;
import com.ymall.backend.home.service.HomeMerchandisingService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;

@SpringBootTest
@ActiveProfiles("test")
class HomeMerchandisingCacheIntegrationTest {

    private static final int MEASUREMENT_COUNT = 20;

    @Autowired private HomeMerchandisingService merchandisingService;
    @Autowired private ProductCacheInvalidator cacheInvalidator;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private StringRedisTemplate redisTemplate;

    private Product product;

    @BeforeEach
    void setUp() {
        clearCache();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        Category category = categoryRepository.save(
            new Category("생활", "living", null, 1, 1, true)
        );
        product = productRepository.save(new Product(
            category,
            "홈 캐시 상품",
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "/images/product.jpg",
            ProductStatus.APPROVED
        ));
    }

    @AfterEach
    void tearDown() {
        clearCache();
    }

    @Test
    void cachesWithFiveMinuteTtlAndEvictsOnProductChange() {
        HomeMerchandisingResponse first = merchandisingService.getMerchandising();

        assertThat(redisTemplate.hasKey(redisKey())).isTrue();
        assertThat(redisTemplate.getExpire(redisKey()))
            .isPositive()
            .isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());

        productRepository.deleteAll();
        assertThat(merchandisingService.getMerchandising()).isEqualTo(first);

        cacheInvalidator.evictDetail(product.getId());
        assertThat(redisTemplate.hasKey(redisKey())).isFalse();
    }

    @Test
    void recordsUncachedAndCachedResponseTime() {
        long uncachedNanos = 0;
        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            clearCache();
            long startedAt = System.nanoTime();
            merchandisingService.getMerchandising();
            uncachedNanos += System.nanoTime() - startedAt;
        }

        merchandisingService.getMerchandising();
        long cachedNanos = 0;
        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            long startedAt = System.nanoTime();
            merchandisingService.getMerchandising();
            cachedNanos += System.nanoTime() - startedAt;
        }

        double uncachedAverageMs = averageMillis(uncachedNanos);
        double cachedAverageMs = averageMillis(cachedNanos);
        System.out.printf(
            "HOME_MERCHANDISING_CACHE_BENCHMARK count=%d uncachedAvgMs=%.3f "
                + "cachedAvgMs=%.3f improvement=%.1f%%%n",
            MEASUREMENT_COUNT,
            uncachedAverageMs,
            cachedAverageMs,
            (1 - cachedAverageMs / uncachedAverageMs) * 100
        );

        assertThat(uncachedAverageMs).isPositive();
        assertThat(cachedAverageMs).isPositive();
    }

    private void clearCache() {
        Cache cache = cacheManager.getCache(HomeCacheNames.MERCHANDISING);
        if (cache != null) {
            cache.invalidate();
        }
    }

    private String redisKey() {
        return HomeCacheNames.MERCHANDISING + "::all";
    }

    private double averageMillis(long nanos) {
        return nanos / (double) MEASUREMENT_COUNT / 1_000_000;
    }
}
