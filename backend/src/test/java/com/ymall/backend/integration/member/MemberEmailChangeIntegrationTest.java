package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.OAuthAccountRepository;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberEmailChangeIntegrationTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OAuthAccountRepository oAuthAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private StringRedisTemplate redisTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        deleteRedisKeys();
        oAuthAccountRepository.deleteAll();
        memberRepository.deleteAll();
        clearInvocations(mailSender);
    }

    @AfterEach
    void tearDown() {
        deleteRedisKeys();
        oAuthAccountRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void localMemberChangesVerifiedEmailAndExistingRefreshTokenIsRevoked() throws Exception {
        Member member = memberRepository.save(new Member(
            "old@example.com",
            passwordEncoder.encode("password123"),
            "Local User",
            MemberRole.ROLE_USER
        ));
        LoginSession login = login("old@example.com", "password123");

        reauthenticate(login.authorization(), "password123")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationRequired").value(false));

        String requestId = requestNewEmail(login.authorization(), "new@example.com");
        String code = capturedVerificationCode();

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, login.authorization())
                .cookie(login.refreshCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "new@example.com", code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getEmail()).isEqualTo("new@example.com");

        mockMvc.perform(post("/api/members/tokens/refresh").cookie(login.refreshCookie()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        loginExpectingStatus("old@example.com", "password123", 401);
        loginExpectingStatus("new@example.com", "password123", 200);
    }

    @Test
    void socialMemberVerifiesCurrentEmailAndKeepsProviderConnection() throws Exception {
        Member member = memberRepository.save(new Member(
            "social-old@example.com",
            null,
            "Social User",
            MemberRole.ROLE_USER
        ));
        oAuthAccountRepository.save(new OAuthAccount(member, OAuthProvider.GOOGLE, "google-sub-123"));
        String authorization = authorization(member);

        MvcResult reauthenticationResult = reauthenticate(authorization, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationRequired").value(true))
            .andExpect(jsonPath("$.data.maskedEmail").value("s***@example.com"))
            .andReturn();
        String currentRequestId = responseValue(reauthenticationResult, "requestId");
        String currentCode = capturedVerificationCode();
        clearInvocations(mailSender);

        mockMvc.perform(post("/api/members/me/email-change/reauthentications/confirm")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(codeJson(currentRequestId, currentCode)))
            .andExpect(status().isOk());

        String newRequestId = requestNewEmail(authorization, "social-new@example.com");
        String newCode = capturedVerificationCode();

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(newRequestId, "social-new@example.com", newCode)))
            .andExpect(status().isOk());

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getEmail()).isEqualTo("social-new@example.com");
        assertThat(oAuthAccountRepository
            .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-123"))
            .get()
            .extracting(OAuthAccount::getMember)
            .extracting(Member::getId)
            .isEqualTo(member.getId());
    }

    @Test
    void finalDuplicateCheckRejectsEmailClaimedAfterCodeWasSent() throws Exception {
        Member member = memberRepository.save(new Member(
            "race-old@example.com",
            passwordEncoder.encode("password123"),
            "Race User",
            MemberRole.ROLE_USER
        ));
        String authorization = authorization(member);
        reauthenticate(authorization, "password123").andExpect(status().isOk());
        String requestId = requestNewEmail(authorization, "race-new@example.com");
        String code = capturedVerificationCode();

        memberRepository.saveAndFlush(new Member(
            "race-new@example.com",
            passwordEncoder.encode("otherPassword123"),
            "Other User",
            MemberRole.ROLE_USER
        ));

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "race-new@example.com", code)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getEmail())
            .isEqualTo("race-old@example.com");
    }

    @Test
    void newEmailVerificationRequiresRecentReauthentication() throws Exception {
        Member member = memberRepository.save(new Member(
            "no-reauth@example.com",
            passwordEncoder.encode("password123"),
            "No Reauth User",
            MemberRole.ROLE_USER
        ));

        mockMvc.perform(post("/api/members/me/email-change/verifications")
                .header(HttpHeaders.AUTHORIZATION, authorization(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"new@example.com"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("EMAIL_CHANGE_REAUTHENTICATION_REQUIRED"));
    }

    private org.springframework.test.web.servlet.ResultActions reauthenticate(
        String authorization,
        String currentPassword
    ) throws Exception {
        String body = currentPassword == null
            ? "{}"
            : """
                {"currentPassword":"%s"}
                """.formatted(currentPassword);
        return mockMvc.perform(post("/api/members/me/email-change/reauthentications")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private String requestNewEmail(String authorization, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/me/email-change/verifications")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestId").isNotEmpty())
            .andReturn();
        return responseValue(result, "requestId");
    }

    private LoginSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        String accessToken = responseValue(result, "accessToken");
        return new LoginSession(
            "Bearer " + accessToken,
            result.getResponse().getCookie(RefreshTokenCookieManager.COOKIE_NAME)
        );
    }

    private void loginExpectingStatus(String email, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password)))
            .andExpect(status().is(expectedStatus));
    }

    private String authorization(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
    }

    private String codeJson(String requestId, String code) {
        return """
            {"requestId":"%s","code":"%s"}
            """.formatted(requestId, code);
    }

    private String changeJson(String requestId, String email, String code) {
        return """
            {"requestId":"%s","email":"%s","code":"%s"}
            """.formatted(requestId, email, code);
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
        deleteKeys("email-change:*");
        deleteKeys("auth:refresh:*");
        deleteKeys("auth:member-refresh:*");
    }

    private void deleteKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private record LoginSession(String authorization, Cookie refreshCookie) {
    }
}
