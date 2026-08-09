package com.ymall.backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import com.ymall.backend.review.config.ReviewSummaryProperties;

class ReviewSummaryGenerationLockManagerTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ReviewSummaryGenerationLockManager lockManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        ReviewSummaryProperties properties = mock(ReviewSummaryProperties.class);
        given(properties.lockTtl()).willReturn(Duration.ofMinutes(1));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        lockManager = new ReviewSummaryGenerationLockManager(redisTemplate, properties);
    }

    @Test
    void releasesRedisLockWithItsOwnershipToken() {
        given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .willReturn(true);

        ReviewSummaryGenerationLockManager.GenerationLock lock = lockManager.acquire(10L)
            .orElseThrow();
        lock.close();

        verify(redisTemplate).execute(
            org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
            org.mockito.ArgumentMatchers.eq(List.of("review-summary:generation:10")),
            anyString()
        );
    }

    @Test
    void releasesLocalFallbackSoTheNextGenerationCanRun() {
        given(redisTemplate.opsForValue())
            .willThrow(new RedisConnectionFailureException("redis unavailable"));

        ReviewSummaryGenerationLockManager.GenerationLock first = lockManager.acquire(10L)
            .orElseThrow();

        assertThat(lockManager.acquire(10L)).isEmpty();

        first.close();

        ReviewSummaryGenerationLockManager.GenerationLock next = lockManager.acquire(10L)
            .orElseThrow();
        next.close();
    }
}
