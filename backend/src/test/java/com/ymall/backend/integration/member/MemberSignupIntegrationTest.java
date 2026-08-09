package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MemberSignupIntegrationTest {

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("(?<!\\d)\\d{6}(?!\\d)");

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        deleteRedisKeys();
        memberRepository.deleteAll();
        clearInvocations(mailSender);
    }

    @AfterEach
    void tearDown() {
        deleteRedisKeys();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 API는 인증된 이메일과 암호화한 비밀번호를 저장한다")
    void signupStoresVerifiedMemberWithEncodedPassword() throws Exception {
        String verificationToken = verifyEmail(" User@Example.com ");

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(" User@Example.com ", verificationToken)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
            .andExpect(jsonPath("$.data.password").doesNotExist());

        assertThat(memberRepository.findAll()).hasSize(1);
        Member savedMember = memberRepository.findAll().get(0);
        assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(savedMember.getEmailVerifiedAt()).isNotNull();
        assertThat(passwordEncoder.matches("Nori7!Cloud", savedMember.getPassword())).isTrue();
        assertThat(savedMember.getPhone()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("회원가입 API는 인증하지 않은 이메일을 거부한다")
    void signupRejectsUnverifiedEmail() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("user@example.com", "invalid-token")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("SIGNUP_EMAIL_VERIFICATION_REQUIRED"));

        assertThat(memberRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("회원가입 인증 토큰은 다른 이메일에 사용할 수 없다")
    void signupRejectsVerificationTokenForDifferentEmail() throws Exception {
        String verificationToken = verifyEmail("first@example.com");

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("other@example.com", verificationToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("SIGNUP_EMAIL_VERIFICATION_REQUIRED"));
    }

    @Test
    @DisplayName("회원가입 인증 토큰은 한 번만 사용할 수 있다")
    void signupConsumesVerificationTokenOnce() throws Exception {
        String verificationToken = verifyEmail("user@example.com");

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("user@example.com", verificationToken)))
            .andExpect(status().isCreated());

        memberRepository.deleteAll();

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("user@example.com", verificationToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("SIGNUP_EMAIL_VERIFICATION_REQUIRED"));
    }

    @Test
    @DisplayName("인증 후 다른 회원이 이메일을 선점하면 최종 회원가입을 거부한다")
    void signupRejectsEmailClaimedAfterVerification() throws Exception {
        String verificationToken = verifyEmail("race@example.com");
        memberRepository.saveAndFlush(new Member(
            "race@example.com",
            passwordEncoder.encode("otherPassword123"),
            "Other User",
            MemberRole.ROLE_USER
        ));

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("RACE@EXAMPLE.COM", verificationToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
    }

    private String verifyEmail(String email) throws Exception {
        String normalizedEmail = email.trim().toLowerCase();
        MvcResult sendResult = mockMvc.perform(post("/api/members/signup/email-verifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s"}
                    """.formatted(email)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestId").isNotEmpty())
            .andReturn();
        String requestId = responseValue(sendResult, "requestId");
        String code = capturedVerificationCode();

        MvcResult confirmResult = mockMvc.perform(
                post("/api/members/signup/email-verifications/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"requestId":"%s","email":"%s","code":"%s"}
                        """.formatted(requestId, normalizedEmail, code)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.verificationToken").isNotEmpty())
            .andReturn();
        clearInvocations(mailSender);
        return responseValue(confirmResult, "verificationToken");
    }

    private String capturedVerificationCode() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String signupJson(String email, String verificationToken) {
        return """
            {
                "email": "%s",
                "emailVerificationToken": "%s",
                "password": "Nori7!Cloud",
                "passwordConfirmation": "Nori7!Cloud",
                "name": "홍길동",
                "phone": "010-1234-5678"
            }
            """.formatted(email, verificationToken);
    }

    private String responseValue(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path(field)
            .asString();
    }

    private void deleteRedisKeys() {
        Set<String> keys = redisTemplate.keys("signup-email:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
