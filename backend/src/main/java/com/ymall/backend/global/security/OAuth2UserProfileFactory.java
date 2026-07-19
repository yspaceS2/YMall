package com.ymall.backend.global.security;

import java.util.Map;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.OAuthProvider;

final class OAuth2UserProfileFactory {

    private OAuth2UserProfileFactory() {
    }

    static OAuth2UserProfile create(OAuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> profile(attributes.get("sub"), attributes.get("email"), attributes.get("name"));
            case KAKAO -> {
                Map<String, Object> account = map(attributes.get("kakao_account"));
                Map<String, Object> profile = map(account.get("profile"));
                yield kakaoProfile(attributes.get("id"), profile.get("nickname"));
            }
            case NAVER -> {
                Map<String, Object> response = map(attributes.get("response"));
                yield profile(response.get("id"), response.get("email"), response.get("name"));
            }
        };
    }

    private static OAuth2UserProfile kakaoProfile(Object id, Object name) {
        if (id == null) {
            throw new BusinessException(ErrorCode.OAUTH_REQUIRED_INFORMATION_MISSING);
        }
        String providerUserId = id.toString();
        String nameValue = name == null || name.toString().isBlank()
            ? "YMall Member"
            : name.toString().trim();
        return new OAuth2UserProfile(providerUserId, null, nameValue);
    }

    private static OAuth2UserProfile profile(Object id, Object email, Object name) {
        if (id == null || email == null) {
            throw new BusinessException(ErrorCode.OAUTH_REQUIRED_INFORMATION_MISSING);
        }
        String emailValue = email.toString().trim().toLowerCase();
        String nameValue = name == null || name.toString().isBlank() ? "YMall Member" : name.toString().trim();
        return new OAuth2UserProfile(id.toString(), emailValue, nameValue);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }
}
