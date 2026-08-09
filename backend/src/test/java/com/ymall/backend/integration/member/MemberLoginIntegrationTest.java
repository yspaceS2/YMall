package com.ymall.backend.integration.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.global.util.SecurityTokenUtils;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        clearLoginAttempts();
        memberRepository.save(new Member(
            "user@example.com",
            passwordEncoder.encode("password123"),
            "홍길동",
            MemberRole.ROLE_USER
        ));
    }

    @AfterEach
    void tearDown() {
        clearLoginAttempts();
    }

    @Test
    void loginReturnsAccessTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("user@example.com", "password123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("user@example.com", "wrong-password")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    void repeatedLoginFailuresAreRateLimitedAndSuccessfulLoginResetsAttempts() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson("user@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("user@example.com", "password123")))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("LOGIN_ATTEMPT_LIMIT_EXCEEDED"));

        clearLoginAttempts();

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("user@example.com", "password123")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("user@example.com", "wrong-password")))
            .andExpect(status().isUnauthorized());
    }

    private void clearLoginAttempts() {
        redisTemplate.delete("login-attempt:" + SecurityTokenUtils.sha256("user@example.com"));
    }

    private String loginJson(String email, String password) {
        return """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(email, password);
    }
}
