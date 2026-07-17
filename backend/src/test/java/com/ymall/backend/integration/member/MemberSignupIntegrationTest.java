package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class MemberSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 API는 정규화한 이메일과 암호화한 비밀번호를 저장한다")
    void signupStoresMemberWithEncodedPassword() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson(" User@Example.com ")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
            .andExpect(jsonPath("$.data.password").doesNotExist());

        assertThat(memberRepository.findAll()).hasSize(1);
        Member savedMember = memberRepository.findAll().get(0);
        assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(savedMember.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedMember.getPassword())).isTrue();
        assertThat(savedMember.getRole()).isEqualTo(MemberRole.ROLE_USER);
    }

    @Test
    @DisplayName("회원가입 API는 이메일 대소문자와 관계없이 중복 가입을 거부한다")
    void signupRejectsDuplicatedEmailIgnoringCase() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("user@example.com")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("USER@EXAMPLE.COM")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));

        assertThat(memberRepository.findAll()).hasSize(1);
    }

    private String signupJson(String email) {
        return """
            {
                "email": "%s",
                "password": "password123",
                "name": "홍길동"
            }
            """.formatted(email);
    }
}
