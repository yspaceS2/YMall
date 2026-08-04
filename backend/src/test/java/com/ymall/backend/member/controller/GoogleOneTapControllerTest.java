package com.ymall.backend.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.ymall.backend.global.security.AuthenticationTokens;
import com.ymall.backend.global.security.GoogleOneTapNonceService;
import com.ymall.backend.global.security.GoogleOneTapProperties;
import com.ymall.backend.global.security.GoogleOneTapTokenVerifier;
import com.ymall.backend.global.security.OAuth2UserProfile;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.global.security.OAuthMemberService;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthProvider;

@WebMvcTest(GoogleOneTapController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoogleOneTapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private GoogleOneTapNonceService nonceService;
    @MockitoBean private GoogleOneTapProperties properties;
    @MockitoBean private GoogleOneTapTokenVerifier tokenVerifier;
    @MockitoBean private OAuthMemberService oAuthMemberService;
    @MockitoBean private OAuthFlowContext oAuthFlowContext;
    @MockitoBean private RefreshTokenService refreshTokenService;
    @MockitoBean private RefreshTokenCookieManager refreshTokenCookieManager;

    private OAuth2UserProfile profile;

    @BeforeEach
    void setUp() {
        profile = new OAuth2UserProfile("google-user", "user@example.com", "Google User");
    }

    @Test
    void issuesNonceWithoutExposingRedisKey() throws Exception {
        given(nonceService.issue()).willReturn("one-time-nonce");
        given(properties.getClientId()).willReturn("google-web-client-id");
        given(properties.getNonceTtl()).willReturn(Duration.ofMinutes(5));

        mockMvc.perform(post("/api/members/oauth2/google/one-tap/nonces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clientId").value("google-web-client-id"))
            .andExpect(jsonPath("$.data.nonce").value("one-time-nonce"))
            .andExpect(jsonPath("$.data.expiresIn").value(300));
    }

    @Test
    void logsInExistingGoogleAccount() throws Exception {
        Member member = new Member(
            "user@example.com",
            null,
            "Google User",
            MemberRole.ROLE_USER
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        given(tokenVerifier.verify("google-credential")).willReturn(profile);
        given(oAuthMemberService.findExistingMember(OAuthProvider.GOOGLE, "google-user"))
            .willReturn(Optional.of(member));
        given(refreshTokenService.issueForLogin(member)).willReturn(new AuthenticationTokens(
            new TokenResponse("access-token", "Bearer", 1800),
            "refresh-token"
        ));

        mockMvc.perform(post("/api/members/oauth2/google/one-tap")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"credential":"google-credential"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.signupRequired").value(false))
            .andExpect(jsonPath("$.data.token.accessToken").value("access-token"));

        verify(refreshTokenCookieManager).write(any(), org.mockito.ArgumentMatchers.eq("refresh-token"));
    }

    @Test
    void startsAdditionalSignupForUnknownGoogleAccount() throws Exception {
        given(tokenVerifier.verify("google-credential")).willReturn(profile);
        given(oAuthMemberService.findExistingMember(OAuthProvider.GOOGLE, "google-user"))
            .willReturn(Optional.empty());

        mockMvc.perform(post("/api/members/oauth2/google/one-tap")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"credential":"google-credential"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.signupRequired").value(true))
            .andExpect(jsonPath("$.data.token").isEmpty());

        verify(oAuthFlowContext).start(any(), org.mockito.ArgumentMatchers.eq(OAuthProvider.GOOGLE), org.mockito.ArgumentMatchers.eq(profile));
    }
}
