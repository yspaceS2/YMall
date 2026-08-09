package com.ymall.backend.product.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.home.config.HomeCacheNames;
import com.ymall.backend.review.config.ReviewSummaryCacheNames;

@ExtendWith(MockitoExtension.class)
class ProductCacheInvalidatorTest {

    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;
    @Mock private Cache homeCache;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void evictsProductDetailOnlyAfterTransactionCommit() {
        when(cacheManager.getCache(ProductCacheNames.DETAILS)).thenReturn(cache);
        when(cacheManager.getCache(ReviewSummaryCacheNames.BY_PRODUCT)).thenReturn(null);
        when(cacheManager.getCache(HomeCacheNames.MERCHANDISING)).thenReturn(homeCache);
        ProductCacheInvalidator invalidator = new ProductCacheInvalidator(cacheManager);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        invalidator.evictDetail(1L);

        verify(cache, never()).evictIfPresent(1L);
        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        verify(cache).evictIfPresent(1L);
        verify(homeCache).invalidate();
    }

    @Test
    void evictsOnlyProductDetailCacheForStockChanges() {
        when(cacheManager.getCache(ProductCacheNames.DETAILS)).thenReturn(cache);
        ProductCacheInvalidator invalidator = new ProductCacheInvalidator(cacheManager);

        invalidator.evictProductDetails(List.of(1L, 1L, 2L));

        verify(cache).evictIfPresent(1L);
        verify(cache).evictIfPresent(2L);
        verify(cacheManager, never()).getCache(ReviewSummaryCacheNames.BY_PRODUCT);
        verify(cacheManager, never()).getCache(HomeCacheNames.MERCHANDISING);
    }

    @Test
    void doesNotPropagateCacheEvictionFailure() {
        when(cacheManager.getCache(ProductCacheNames.DETAILS)).thenReturn(cache);
        doThrow(new IllegalStateException("Redis unavailable"))
            .when(cache).evictIfPresent(1L);
        ProductCacheInvalidator invalidator = new ProductCacheInvalidator(cacheManager);

        assertThatCode(() -> invalidator.evictProductDetails(List.of(1L)))
            .doesNotThrowAnyException();
    }
}
