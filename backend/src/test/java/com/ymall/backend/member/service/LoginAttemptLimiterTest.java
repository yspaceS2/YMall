package com.ymall.backend.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.util.SecurityTokenUtils;
import com.ymall.backend.member.config.LoginAttemptProperties;

@ExtendWith(MockitoExtension.class)
class LoginAttemptLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        LoginAttemptProperties properties = new LoginAttemptProperties();
        properties.setMaxAttempts(5);
        properties.setWindow(Duration.ofMinutes(15));
        limiter = new LoginAttemptLimiter(redisTemplate, properties);
    }

    @Test
    void allowsAttemptsWithinConfiguredLimit() {
        given(redisTemplate.execute(anyRedisScript(), anyList(), eq("900")))
            .willReturn(5L);

        limiter.consume("user@example.com");
    }

    @Test
    void rejectsAttemptsOverConfiguredLimit() {
        given(redisTemplate.execute(anyRedisScript(), anyList(), eq("900")))
            .willReturn(6L);

        assertThatThrownBy(() -> limiter.consume("user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
    }

    @Test
    void resetsOnlyHashedAccountKeyAfterSuccessfulLogin() {
        limiter.reset("user@example.com");

        verify(redisTemplate).delete(
            "login-attempt:" + SecurityTokenUtils.sha256("user@example.com")
        );
    }

    private RedisScript<Long> anyRedisScript() {
        return any();
    }
}
