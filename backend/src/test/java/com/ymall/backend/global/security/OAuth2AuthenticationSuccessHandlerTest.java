package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthProvider;

class OAuth2AuthenticationSuccessHandlerTest {

    @Test
    void redirectsOidcPrincipalWithYmallAccessToken() throws Exception {
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
        RefreshTokenCookieManager cookieManager = mock(RefreshTokenCookieManager.class);
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(
            refreshTokenService,
            cookieManager,
            mock(OAuthFlowContext.class)
        );
        ReflectionTestUtils.setField(
            handler,
            "frontendRedirectUri",
            "http://localhost:5173/oauth2/callback"
        );
        Member member = new Member("oauth@example.com", "password", "OAuth User", MemberRole.ROLE_USER);
        ReflectionTestUtils.setField(member, "id", 1L);
        OidcUser delegate = mock(OidcUser.class);
        given(delegate.getClaims()).willReturn(Map.of("sub", "google-1"));
        YMallOidcUser principal = new YMallOidcUser(
            member,
            delegate,
            OAuthProvider.GOOGLE,
            new OAuth2UserProfile("google-1", "oauth@example.com", "OAuth User")
        );
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(principal);
        given(refreshTokenService.issue(member)).willReturn(new AuthenticationTokens(
            new TokenResponse("ymall-token", "Bearer", 1800L),
            "refresh-token"
        ));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
            new MockHttpServletRequest(),
            response,
            authentication
        );

        assertThat(response.getRedirectedUrl())
            .isEqualTo("http://localhost:5173/oauth2/callback#accessToken=ymall-token");
    }
}
