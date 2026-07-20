package com.ymall.backend.integration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RefreshTokenIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        deleteRefreshTokens();
        memberRepository.save(new Member(
            "refresh@example.com",
            passwordEncoder.encode("password123"),
            "Refresh User",
            MemberRole.ROLE_USER
        ));
    }

    @AfterEach
    void tearDown() {
        deleteRefreshTokens();
    }

    @Test
    void loginRefreshRotationAndLogoutManageDeviceSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"refresh@example.com","password":"password123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn();
        Cookie firstCookie = loginResult.getResponse().getCookie(RefreshTokenCookieManager.COOKIE_NAME);
        assertSecureCookie(firstCookie);
        assertThat(refreshTokenTtl()).isPositive();

        mockMvc.perform(post("/api/members/login")
                .cookie(firstCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"refresh@example.com","password":"wrong-password"}
                    """))
            .andExpect(status().isUnauthorized());

        MvcResult refreshResult = mockMvc.perform(post("/api/members/tokens/refresh").cookie(firstCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andReturn();
        Cookie rotatedCookie = refreshResult.getResponse().getCookie(RefreshTokenCookieManager.COOKIE_NAME);
        assertSecureCookie(rotatedCookie);
        assertThat(rotatedCookie.getValue()).isNotEqualTo(firstCookie.getValue());

        mockMvc.perform(post("/api/members/tokens/refresh").cookie(firstCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/members/logout").cookie(rotatedCookie))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/tokens/refresh").cookie(rotatedCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    private void assertSecureCookie(Cookie cookie) {
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    private long refreshTokenTtl() {
        Set<String> keys = redisTemplate.keys("auth:refresh:*");
        assertThat(keys).hasSize(1);
        return redisTemplate.getExpire(keys.iterator().next());
    }

    private void deleteRefreshTokens() {
        Set<String> keys = redisTemplate.keys("auth:refresh:*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
