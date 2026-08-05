package com.ymall.backend.review.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.ymall.backend.review.config.ReviewSummaryProperties;

/**
 * Coordinates summary generation with a token-owned Redis lock and configured TTL.
 * When Redis is unavailable, the local fallback prevents duplicates only within
 * the current application instance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryGenerationLockManager {

    private static final String LOCK_KEY_PREFIX = "review-summary:generation:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
        new DefaultRedisScript<>(
            """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """,
            Long.class
        );

    private final StringRedisTemplate redisTemplate;
    private final ReviewSummaryProperties properties;
    private final Set<Long> localLocks = ConcurrentHashMap.newKeySet();

    public Optional<GenerationLock> acquire(Long productId) {
        String key = LOCK_KEY_PREFIX + productId;
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, token, properties.lockTtl());
            return Boolean.TRUE.equals(acquired)
                ? Optional.of(new LockHandle(productId, key, token, true))
                : Optional.empty();
        } catch (RuntimeException exception) {
            log.warn(
                "Redis review summary lock is unavailable. Falling back to a local lock. productId={}",
                productId
            );
            return localLocks.add(productId)
                ? Optional.of(new LockHandle(productId, key, token, false))
                : Optional.empty();
        }
    }

    public interface GenerationLock extends AutoCloseable {

        @Override
        void close();
    }

    private final class LockHandle implements GenerationLock {

        private final Long productId;
        private final String key;
        private final String token;
        private final boolean redis;

        private LockHandle(Long productId, String key, String token, boolean redis) {
            this.productId = productId;
            this.key = key;
            this.token = token;
            this.redis = redis;
        }

        @Override
        public void close() {
            if (!redis) {
                localLocks.remove(productId);
                return;
            }
            try {
                redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), token);
            } catch (RuntimeException exception) {
                log.warn("Redis review summary lock release failed. productId={}", productId);
            }
        }
    }
}
