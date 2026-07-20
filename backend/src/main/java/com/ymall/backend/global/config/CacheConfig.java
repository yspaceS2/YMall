package com.ymall.backend.global.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final RedisConnectionFactory connectionFactory;
    private final GenericJacksonJsonRedisSerializer valueSerializer;
    private final Duration productDetailTtl;

    public CacheConfig(
        RedisConnectionFactory connectionFactory,
        GenericJacksonJsonRedisSerializer valueSerializer,
        @Value("${ymall.cache.product-detail-ttl:10m}") Duration productDetailTtl
    ) {
        this.connectionFactory = connectionFactory;
        this.valueSerializer = valueSerializer;
        this.productDetailTtl = productDetailTtl;
    }

    @Override
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
            );
        RedisCacheConfiguration productDetails = defaults.entryTtl(productDetailTtl);

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaults)
            .withCacheConfiguration(ProductCacheNames.DETAILS, productDetails)
            .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache read failed. Falling back to database. cache={}, key={}, reason={}",
                    cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(
                RuntimeException exception,
                Cache cache,
                Object key,
                Object value
            ) {
                log.warn("Cache write failed. Response remains available. cache={}, key={}, reason={}",
                    cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache eviction failed. cache={}, key={}, reason={}",
                    cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear failed. cache={}, reason={}",
                    cache.getName(), exception.getMessage());
            }
        };
    }
}
