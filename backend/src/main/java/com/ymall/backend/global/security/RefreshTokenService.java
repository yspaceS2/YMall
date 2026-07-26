package com.ymall.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

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
            member.getId().toString(),
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
        String memberId = redisTemplate.opsForValue().getAndDelete(tokenKey);
        if (memberId == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Member member;
        try {
            Long parsedMemberId = Long.valueOf(memberId);
            redisTemplate.opsForSet().remove(memberKey(parsedMemberId), tokenKey);
            member = memberRepository.findById(parsedMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issue(member);
    }

    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenKey = key(refreshToken);
            String memberId = redisTemplate.opsForValue().getAndDelete(tokenKey);
            if (memberId != null) {
                try {
                    redisTemplate.opsForSet().remove(memberKey(Long.valueOf(memberId)), tokenKey);
                } catch (NumberFormatException ignored) {
                    // Invalid Redis data is already revoked by deleting the token key.
                }
            }
        }
    }

    public void revokeAll(Long memberId) {
        String memberKey = memberKey(memberId);
        Set<String> tokenKeys = redisTemplate.opsForSet().members(memberKey);
        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            redisTemplate.delete(tokenKeys);
        }
        redisTemplate.delete(memberKey);
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
}
