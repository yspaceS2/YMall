package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.OAuthProvider;

class OAuth2UserProfileFactoryTest {

    @Test
    void mapsGoogleProfile() {
        OAuth2UserProfile profile = OAuth2UserProfileFactory.create(
            OAuthProvider.GOOGLE,
            Map.of("sub", "google-1", "email", "USER@example.com", "name", "Google User")
        );

        assertThat(profile).isEqualTo(new OAuth2UserProfile(
            "google-1", "user@example.com", "Google User"
        ));
    }

    @Test
    void mapsKakaoProfile() {
        OAuth2UserProfile profile = OAuth2UserProfileFactory.create(
            OAuthProvider.KAKAO,
            Map.of(
                "id", 123L,
                "kakao_account", Map.of(
                    "profile", Map.of("nickname", "Kakao User")
                )
            )
        );

        assertThat(profile).isEqualTo(new OAuth2UserProfile(
            "123", null, "Kakao User"
        ));
    }

    @Test
    void mapsNaverProfile() {
        OAuth2UserProfile profile = OAuth2UserProfileFactory.create(
            OAuthProvider.NAVER,
            Map.of("response", Map.of(
                "id", "naver-1",
                "email", "naver@example.com",
                "name", "Naver User"
            ))
        );

        assertThat(profile).isEqualTo(new OAuth2UserProfile(
            "naver-1", "naver@example.com", "Naver User"
        ));
    }

    @Test
    void rejectsProfileWithoutEmail() {
        assertThatThrownBy(() -> OAuth2UserProfileFactory.create(
            OAuthProvider.GOOGLE,
            Map.of("sub", "google-1", "name", "Google User")
        ))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.OAUTH_REQUIRED_INFORMATION_MISSING);
    }
}
