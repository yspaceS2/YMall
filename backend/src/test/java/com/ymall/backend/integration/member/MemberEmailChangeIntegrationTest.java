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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.OAuthAccountRepository;
import com.ymall.backend.member.service.MemberEmailChangeService;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberEmailChangeIntegrationTest {

    private static final String PASSWORD = "password123";
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OAuthAccountRepository oAuthAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private OAuthFlowContext oAuthFlowContext;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private MemberEmailChangeService emailChangeService;

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
        Member member = memberRepository.save(localMember("old@example.com", "Local User"));
        LoginSession login = login("old@example.com", PASSWORD);
        MockHttpSession session = new MockHttpSession();

        reauthenticate(login.authorization(), PASSWORD, session)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationRequired").value(false));

        String requestId = requestNewEmail(login.authorization(), "new@example.com", session);
        String code = capturedVerificationCode();

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, login.authorization())
                .cookie(login.refreshCookie())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "new@example.com", code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getEmail()).isEqualTo("new@example.com");

        mockMvc.perform(post("/api/members/tokens/refresh").cookie(login.refreshCookie()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
        loginExpectingStatus("old@example.com", PASSWORD, 401);
        loginExpectingStatus("new@example.com", PASSWORD, 200);
    }

    @Test
    void socialMemberChangesEmailAfterOAuthReauthenticationAndKeepsProviderConnection()
        throws Exception {
        Member member = memberRepository.save(socialMember(
            "social-old@example.com",
            "Social User"
        ));
        oAuthAccountRepository.save(new OAuthAccount(member, OAuthProvider.GOOGLE, "google-sub-123"));
        String authorization = authorization(member);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post(
                "/api/members/me/email-change/oauth-reauthentications/google"
            )
                .session(session)
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.authorizationUrl")
                .value("/oauth2/authorization/google"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        emailChangeService.markOAuthReauthenticated(
            member.getId(),
            oAuthFlowContext.getEmailChangeSessionBinding(request)
        );

        String newRequestId = requestNewEmail(
            authorization,
            "social-new@example.com",
            session
        );
        String newCode = capturedVerificationCode();

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(session)
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
        Member member = memberRepository.save(localMember("race-old@example.com", "Race User"));
        String authorization = authorization(member);
        MockHttpSession session = new MockHttpSession();
        reauthenticate(authorization, PASSWORD, session).andExpect(status().isOk());
        String requestId = requestNewEmail(
            authorization,
            "race-new@example.com",
            session
        );
        String code = capturedVerificationCode();

        memberRepository.saveAndFlush(localMember(
            "race-new@example.com",
            "Other User",
            "otherPassword123"
        ));

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "race-new@example.com", code)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getEmail())
            .isEqualTo("race-old@example.com");
    }

    @Test
    void newEmailVerificationRequiresRecentReauthentication() throws Exception {
        Member member = memberRepository.save(localMember(
            "no-reauth@example.com",
            "No Reauth User"
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

    @Test
    void socialReauthenticationRejectsProviderNotLinkedToCurrentMember() throws Exception {
        Member member = memberRepository.save(socialMember("social@example.com", "Social User"));
        oAuthAccountRepository.save(new OAuthAccount(
            member,
            OAuthProvider.GOOGLE,
            "google-sub-123"
        ));

        mockMvc.perform(post(
                "/api/members/me/email-change/oauth-reauthentications/kakao"
            )
                .header(HttpHeaders.AUTHORIZATION, authorization(member)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code")
                .value("EMAIL_CHANGE_OAUTH_ACCOUNT_MISMATCH"));
    }

    @Test
    void reauthenticationCannotBeReusedFromAnotherSession() throws Exception {
        Member member = memberRepository.save(localMember(
            "session-bound@example.com",
            "Session Bound User"
        ));
        String authorization = authorization(member);
        MockHttpSession reauthenticatedSession = new MockHttpSession();
        MockHttpSession otherSession = new MockHttpSession();

        reauthenticate(authorization, PASSWORD, reauthenticatedSession)
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/me/email-change/verifications")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(otherSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"new@example.com"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code")
                .value("EMAIL_CHANGE_REAUTHENTICATION_REQUIRED"));
    }

    @Test
    void expiredReauthenticationDoesNotConsumeValidEmailCode() throws Exception {
        Member member = memberRepository.save(localMember("retry@example.com", "Retry User"));
        String authorization = authorization(member);
        MockHttpSession session = new MockHttpSession();
        reauthenticate(authorization, PASSWORD, session).andExpect(status().isOk());
        String requestId = requestNewEmail(authorization, "retry-new@example.com", session);
        String code = capturedVerificationCode();
        deleteKeys("email-change:reauthenticated:*");

        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "retry-new@example.com", code)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code")
                .value("EMAIL_CHANGE_REAUTHENTICATION_REQUIRED"));

        reauthenticate(authorization, PASSWORD, session).andExpect(status().isOk());
        mockMvc.perform(patch("/api/members/me/email-change")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(changeJson(requestId, "retry-new@example.com", code)))
            .andExpect(status().isOk());

        assertThat(memberRepository.findById(member.getId()).orElseThrow().getEmail())
            .isEqualTo("retry-new@example.com");
    }

    private org.springframework.test.web.servlet.ResultActions reauthenticate(
        String authorization,
        String currentPassword,
        MockHttpSession session
    ) throws Exception {
        String body = currentPassword == null
            ? "{}"
            : """
                {"currentPassword":"%s"}
                """.formatted(currentPassword);
        return mockMvc.perform(post("/api/members/me/email-change/reauthentications")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private String requestNewEmail(
        String authorization,
        String email,
        MockHttpSession session
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/members/me/email-change/verifications")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .session(session)
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

    private Member localMember(String email, String name) {
        return localMember(email, name, PASSWORD);
    }

    private Member localMember(String email, String name, String password) {
        return new Member(
            email,
            passwordEncoder.encode(password),
            name,
            MemberRole.ROLE_USER
        );
    }

    private Member socialMember(String email, String name) {
        return new Member(email, null, name, MemberRole.ROLE_USER);
    }

    private String loginJson(String email, String password) {
        return """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);
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
