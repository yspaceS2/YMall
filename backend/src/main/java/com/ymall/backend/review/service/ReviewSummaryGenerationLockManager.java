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
 * Token 소유권과 설정된 TTL을 사용하는 Redis 잠금으로 리뷰 요약 생성을 조정한다.
 *
 * <p>해제 Script는 자신이 획득한 Token과 일치할 때만 잠금을 삭제하여, TTL 만료 후 다른 작업이
 * 획득한 잠금을 지우지 않게 한다. Redis 장애 시 Local 잠금으로 대체하지만 이 경우 중복 방지는
 * 현재 애플리케이션 Instance 안에서만 보장된다.</p>
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

    /**
     * 상품별 생성 잠금을 획득하고, 이미 실행 중이면 빈 값을 반환한다.
     * 반환된 잠금은 try-with-resources로 반드시 해제해야 한다.
     */
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
