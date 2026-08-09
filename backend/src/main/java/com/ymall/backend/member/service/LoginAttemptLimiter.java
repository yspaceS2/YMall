package com.ymall.backend.member.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.util.SecurityTokenUtils;
import com.ymall.backend.member.config.LoginAttemptProperties;

@Service
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    private static final String KEY_PREFIX = "login-attempt:";
    private static final DefaultRedisScript<Long> CONSUME_ATTEMPT_SCRIPT = new DefaultRedisScript<>(
        """
        local attempts = redis.call('INCR', KEYS[1])
        if attempts == 1 then
            redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        return attempts
        """,
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final LoginAttemptProperties properties;

    public void consume(String normalizedEmail) {
        Long attempts = redisTemplate.execute(
            CONSUME_ATTEMPT_SCRIPT,
            List.of(key(normalizedEmail)),
            Long.toString(properties.getWindow().toSeconds())
        );
        if (attempts == null || attempts > properties.getMaxAttempts()) {
            throw new BusinessException(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED);
        }
    }

    public void reset(String normalizedEmail) {
        redisTemplate.delete(key(normalizedEmail));
    }

    private String key(String normalizedEmail) {
        return KEY_PREFIX + SecurityTokenUtils.sha256(normalizedEmail);
    }
}
