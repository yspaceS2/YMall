package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StringRedisTemplate redisTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        deleteRedisKeys();
    }

    @AfterEach
    void tearDown() {
        deleteRedisKeys();
    }

    @Test
    void resetsLocalPasswordOnceAndRevokesExistingRefreshTokens() throws Exception {
        Member member = memberRepository.save(new Member(
            "reset@example.com",
            passwordEncoder.encode("password123"),
            "Reset User",
            MemberRole.ROLE_USER
        ));
        Cookie oldRefreshCookie = login("reset@example.com", "password123");

        MvcResult requestResult = requestReset("reset@example.com")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestId").isNotEmpty())
            .andReturn();
        String requestId = responseValue(requestResult, "requestId");
        String verificationCode = capturedVerificationCode();

        MvcResult verificationResult = mockMvc.perform(post("/api/members/password-reset-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestId":"%s","code":"%s"}
                    """.formatted(requestId, verificationCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
            .andExpect(jsonPath("$.data.expiresIn").value(600))
            .andReturn();
        String resetToken = responseValue(verificationResult, "resetToken");

        mockMvc.perform(post("/api/members/password-resets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "resetToken":"%s",
                        "newPassword":"newPassword123",
                        "newPasswordConfirmation":"newPassword123"
                    }
                    """.formatted(resetToken)))
            .andExpect(status().isOk());

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword123", updatedMember.getPassword())).isTrue();

        mockMvc.perform(post("/api/members/tokens/refresh").cookie(oldRefreshCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/members/password-resets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "resetToken":"%s",
                        "newPassword":"anotherPassword123",
                        "newPasswordConfirmation":"anotherPassword123"
                    }
                    """.formatted(resetToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));

        loginExpectingStatus("reset@example.com", "password123", 401);
        loginExpectingStatus("reset@example.com", "newPassword123", 200);
    }

    @Test
    void requestDoesNotRevealUnknownOrSocialOnlyAccount() throws Exception {
        memberRepository.save(new Member(
            "social@example.com",
            null,
            "Social User",
            MemberRole.ROLE_USER
        ));

        requestReset("unknown@example.com")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("가입된 일반 회원 계정이라면 인증번호를 전송했습니다."));
        requestReset("social@example.com")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("가입된 일반 회원 계정이라면 인증번호를 전송했습니다."));

        verifyNoInteractions(mailSender);
    }

    @Test
    void invalidCodeCannotBeRetriedAfterMaximumAttempts() throws Exception {
        memberRepository.save(new Member(
            "attempts@example.com",
            passwordEncoder.encode("password123"),
            "Attempts User",
            MemberRole.ROLE_USER
        ));
        String requestId = responseValue(
            requestReset("attempts@example.com").andReturn(),
            "requestId"
        );
        String correctCode = capturedVerificationCode();

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/members/password-reset-verifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"requestId":"%s","code":"000000"}
                        """.formatted(requestId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_VERIFICATION_FAILED"));
        }

        mockMvc.perform(post("/api/members/password-reset-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestId":"%s","code":"%s"}
                    """.formatted(requestId, correctCode)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_VERIFICATION_FAILED"));
    }

    @Test
    void repeatedRequestIsRateLimitedWithoutRevealingAccountExistence() throws Exception {
        requestReset("limited@example.com").andExpect(status().isOk());

        requestReset("limited@example.com")
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_REQUEST_LIMIT_EXCEEDED"));
    }

    @Test
    void mailFailureKeepsGenericResponseAndInvalidatesChallenge() throws Exception {
        memberRepository.save(new Member(
            "mail-failure@example.com",
            passwordEncoder.encode("password123"),
            "Mail Failure User",
            MemberRole.ROLE_USER
        ));
        doThrow(new MailSendException("delivery failed"))
            .when(mailSender)
            .send(any(SimpleMailMessage.class));

        String requestId = responseValue(
            requestReset("mail-failure@example.com")
                .andExpect(status().isOk())
                .andReturn(),
            "requestId"
        );

        mockMvc.perform(post("/api/members/password-reset-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestId":"%s","code":"000000"}
                    """.formatted(requestId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_VERIFICATION_FAILED"));
    }

    private org.springframework.test.web.servlet.ResultActions requestReset(String email) throws Exception {
        return mockMvc.perform(post("/api/members/password-reset-requests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s"}
                """.formatted(email)));
    }

    private Cookie login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return result.getResponse().getCookie(RefreshTokenCookieManager.COOKIE_NAME);
    }

    private void loginExpectingStatus(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().is(expectedStatus));
    }

    private String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }

    private String responseValue(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path(field)
            .asString();
    }

    private String capturedVerificationCode() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private void deleteRedisKeys() {
        deleteKeys("password-reset:*");
        deleteKeys("auth:refresh:*");
        deleteKeys("auth:member-refresh:*");
    }

    private void deleteKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
