package com.ymall.backend.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.review.config.ReviewSummaryCacheNames;

@Component
public class ProductCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheInvalidator.class);

    private final CacheManager cacheManager;

    public ProductCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictDetail(Long productId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(productId);
                }
            });
            return;
        }
        evict(productId);
    }

    private void evict(Long productId) {
        evict(ProductCacheNames.DETAILS, productId);
        evict(ReviewSummaryCacheNames.BY_PRODUCT, productId);
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
}
