package com.ymall.backend.global.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.util.SecurityTokenUtils;

@Service
@RequiredArgsConstructor
public class GoogleOneTapNonceService {

    private static final String NONCE_KEY_PREFIX = "auth:google-one-tap:nonce:";
    private final StringRedisTemplate redisTemplate;
    private final GoogleOneTapProperties properties;

    public String issue() {
        String nonce = SecurityTokenUtils.generateUrlSafeToken();
        redisTemplate.opsForValue().set(key(nonce), "1", properties.getNonceTtl());
        return nonce;
    }

    public void consume(String nonce) {
        if (nonce == null
            || nonce.isBlank()
            || redisTemplate.opsForValue().getAndDelete(key(nonce)) == null) {
            throw new BusinessException(ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID);
        }
    }

    private String key(String nonce) {
        return NONCE_KEY_PREFIX + SecurityTokenUtils.sha256(nonce);
    }
}
