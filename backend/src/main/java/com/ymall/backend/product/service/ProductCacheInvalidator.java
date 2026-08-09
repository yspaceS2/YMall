package com.ymall.backend.product.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.home.config.HomeCacheNames;
import com.ymall.backend.review.config.ReviewSummaryCacheNames;

/**
 * 상품 변경 트랜잭션이 커밋된 뒤 관련 조회 캐시를 무효화한다.
 *
 * <p>롤백된 변경 때문에 정상 캐시가 삭제되지 않도록 활성 트랜잭션에서는 afterCommit 시점까지
 * 무효화를 미룬다. 캐시 장애는 원장 데이터 변경을 실패시키지 않으며, 상세 상품 변경은 리뷰 요약과
 * 홈 진열 캐시에도 영향을 줄 수 있으므로 함께 제거한다.</p>
 */
@Component
public class ProductCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidator.class);

    private final CacheManager cacheManager;

    public ProductCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictDetail(Long productId) {
        executeAfterCommit(() -> evict(productId));
    }

    public void evictProductDetails(Collection<Long> productIds) {
        Set<Long> uniqueProductIds = new LinkedHashSet<>(productIds);
        executeAfterCommit(() ->
            uniqueProductIds.forEach(productId ->
                evict(ProductCacheNames.DETAILS, productId)
            )
        );
    }

    private void executeAfterCommit(Runnable eviction) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
            return;
        }
        eviction.run();
    }

    private void evict(Long productId) {
        evict(ProductCacheNames.DETAILS, productId);
        evict(ReviewSummaryCacheNames.BY_PRODUCT, productId);
        clear(HomeCacheNames.MERCHANDISING);
    }

    private void evict(String cacheName, Long productId) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        try {
            cache.evictIfPresent(productId);
        } catch (RuntimeException exception) {
            log.warn("Product cache eviction failed. cache={}, productId={}, reason={}",
                cacheName, productId, exception.getMessage());
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }
        try {
            cache.invalidate();
        } catch (RuntimeException exception) {
            log.warn("Cache clear failed. cache={}, reason={}",
                cacheName, exception.getMessage());
        }
    }
}
