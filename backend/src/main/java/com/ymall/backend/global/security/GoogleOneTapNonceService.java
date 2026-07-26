package com.ymall.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class GoogleOneTapNonceService {

    private static final String NONCE_KEY_PREFIX = "auth:google-one-tap:nonce:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final GoogleOneTapProperties properties;

    public String issue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(nonce.getBytes(StandardCharsets.UTF_8));
            return NONCE_KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
