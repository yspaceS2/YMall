package com.ymall.backend.product.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import com.ymall.backend.global.config.ProductCacheNames;

@Component
public class ProductCacheInvalidator {

    @CacheEvict(
        cacheNames = ProductCacheNames.DETAILS,
        key = "#productId",
        beforeInvocation = true
    )
    public void evictDetail(Long productId) {
        // Cache eviction is performed by Spring's cache interceptor.
    }
}
