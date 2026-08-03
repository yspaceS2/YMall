package com.ymall.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String MEMBER_KEY_PREFIX = "auth:member-refresh:";
    private static final String VALUE_SEPARATOR = ":";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    public AuthenticationTokens issue(Member member) {
        String refreshToken = generateToken();
        String tokenKey = key(refreshToken);
        String memberKey = memberKey(member.getId());
        redisTemplate.opsForValue().set(
            tokenKey,
            tokenValue(member),
            jwtProperties.getRefreshTokenExpiration()
        );
        redisTemplate.opsForSet().add(memberKey, tokenKey);
        redisTemplate.expire(memberKey, jwtProperties.getRefreshTokenExpiration());
        return new AuthenticationTokens(jwtTokenProvider.createAccessToken(member), refreshToken);
    }

    public AuthenticationTokens rotate(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String tokenKey = key(refreshToken);
        String storedValue = redisTemplate.opsForValue().getAndDelete(tokenKey);
        if (storedValue == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Member member;
        try {
            String[] valueParts = storedValue.split(VALUE_SEPARATOR, -1);
            if (valueParts.length != 2) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            Long parsedMemberId = Long.valueOf(valueParts[0]);
            long issuedAuthVersion = Long.parseLong(valueParts[1]);
            redisTemplate.opsForSet().remove(memberKey(parsedMemberId), tokenKey);
            member = memberRepository.findById(parsedMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
            if (member.getAuthVersion() != issuedAuthVersion) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issue(member);
    }

    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenKey = key(refreshToken);
            String storedValue = redisTemplate.opsForValue().getAndDelete(tokenKey);
            if (storedValue != null) {
                try {
                    Long memberId = Long.valueOf(storedValue.split(VALUE_SEPARATOR, -1)[0]);
                    redisTemplate.opsForSet().remove(memberKey(memberId), tokenKey);
                } catch (NumberFormatException ignored) {
                    // Invalid Redis data is already revoked by deleting the token key.
                }
            }
        }
    }

    public void revokeAll(Long memberId) {
        String memberKey = memberKey(memberId);
        Set<String> tokenKeys = new HashSet<>();
        Set<String> indexedTokenKeys = redisTemplate.opsForSet().members(memberKey);
        if (indexedTokenKeys != null) {
            tokenKeys.addAll(indexedTokenKeys);
        }
        tokenKeys.addAll(findLegacyTokenKeys(memberId));
        if (!tokenKeys.isEmpty()) {
            redisTemplate.delete(tokenKeys);
        }
        redisTemplate.delete(memberKey);
    }

    private Set<String> findLegacyTokenKeys(Long memberId) {
        Set<String> tokenKeys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .match(KEY_PREFIX + "*")
            .count(100)
            .build();
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            cursor.forEachRemaining(tokenKey -> {
                String storedValue = redisTemplate.opsForValue().get(tokenKey);
                if (memberId.toString().equals(storedValue)
                    || (storedValue != null
                    && storedValue.startsWith(memberId + VALUE_SEPARATOR))) {
                    tokenKeys.add(tokenKey);
                }
            });
        }
        return tokenKeys;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String key(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String memberKey(Long memberId) {
        return MEMBER_KEY_PREFIX + memberId;
    }

    private String tokenValue(Member member) {
        return member.getId() + VALUE_SEPARATOR + member.getAuthVersion();
    }
}
