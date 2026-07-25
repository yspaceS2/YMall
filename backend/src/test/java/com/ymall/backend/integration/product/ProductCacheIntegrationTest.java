package com.ymall.backend.integration.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductService;
import com.ymall.backend.review.config.ReviewSummaryCacheNames;

@SpringBootTest
@ActiveProfiles("test")
class ProductCacheIntegrationTest {

    private static final int MEASUREMENT_COUNT = 30;

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private StringRedisTemplate redisTemplate;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        clearProductCache();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        category = categoryRepository.save(new Category("캐시 테스트", "cache-test"));
        product = productRepository.save(product("캐시 상품", ProductStatus.APPROVED));
    }

    @AfterEach
    void tearDown() {
        clearProductCache();
    }

    @Test
    void cachesProductDetailWithTtl() {
        ProductDetailResponse first = productService.getProduct(product.getId());
        String redisKey = redisKey(product.getId());

        assertThat(redisTemplate.hasKey(redisKey)).isTrue();
        assertThat(redisTemplate.getExpire(redisKey))
            .isPositive()
            .isLessThanOrEqualTo(Duration.ofMinutes(10).toSeconds());

        productRepository.deleteAll();

        ProductDetailResponse cached = productService.getProduct(product.getId());
        assertThat(cached).isEqualTo(first);
    }

    @Test
    void evictsCachedDetailWhenProductIsUpdatedOrDeleted() {
        productService.getProduct(product.getId());
        cacheManager.getCache(ReviewSummaryCacheNames.BY_PRODUCT)
            .put(product.getId(), "cached summary");
        assertThat(redisTemplate.hasKey(redisKey(product.getId()))).isTrue();
        assertThat(redisTemplate.hasKey(reviewSummaryRedisKey(product.getId()))).isTrue();

        productService.updateProduct(product.getId(), new ProductUpdateRequest(
            category.getId(),
            "수정된 캐시 상품",
            "수정된 설명",
            "YMall",
            BigDecimal.valueOf(20000),
            BigDecimal.ZERO,
            5,
            null,
            List.of()
        ));

        assertThat(redisTemplate.hasKey(redisKey(product.getId()))).isFalse();
        assertThat(redisTemplate.hasKey(reviewSummaryRedisKey(product.getId()))).isFalse();
        assertThat(productService.getProduct(product.getId()).name()).isEqualTo("수정된 캐시 상품");

        cacheManager.getCache(ReviewSummaryCacheNames.BY_PRODUCT)
            .put(product.getId(), "cached summary");
        productService.deleteProduct(product.getId());
        assertThat(redisTemplate.hasKey(redisKey(product.getId()))).isFalse();
        assertThat(redisTemplate.hasKey(reviewSummaryRedisKey(product.getId()))).isFalse();
    }

    @Test
    void comparesUncachedAndCachedLookupTime() {
        long uncachedNanos = 0;
        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            clearProductCache();
            long startedAt = System.nanoTime();
            productService.getProduct(product.getId());
            uncachedNanos += System.nanoTime() - startedAt;
        }

        productService.getProduct(product.getId());
        long cachedNanos = 0;
        for (int index = 0; index < MEASUREMENT_COUNT; index++) {
            long startedAt = System.nanoTime();
            productService.getProduct(product.getId());
            cachedNanos += System.nanoTime() - startedAt;
        }

        double uncachedAverageMs = nanosToAverageMillis(uncachedNanos);
        double cachedAverageMs = nanosToAverageMillis(cachedNanos);
        System.out.printf(
            "PRODUCT_CACHE_BENCHMARK count=%d uncachedAvgMs=%.3f cachedAvgMs=%.3f improvement=%.1f%%%n",
            MEASUREMENT_COUNT,
            uncachedAverageMs,
            cachedAverageMs,
            (1 - cachedAverageMs / uncachedAverageMs) * 100
        );

        assertThat(uncachedAverageMs).isPositive();
        assertThat(cachedAverageMs).isPositive();
    }

    private Product product(String name, ProductStatus status) {
        return new Product(
            category,
            name,
            "캐시 검증용 상품",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            status
        );
    }

    private void clearProductCache() {
        for (String cacheName : List.of(
            ProductCacheNames.DETAILS,
            ReviewSummaryCacheNames.BY_PRODUCT
        )) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private String redisKey(Long productId) {
        return ProductCacheNames.DETAILS + "::" + productId;
    }

    private String reviewSummaryRedisKey(Long productId) {
        return ReviewSummaryCacheNames.BY_PRODUCT + "::" + productId;
    }

    private double nanosToAverageMillis(long nanos) {
        return nanos / (double) MEASUREMENT_COUNT / 1_000_000;
    }
}
