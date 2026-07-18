package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private String authorization;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member(
            "profile@example.com",
            passwordEncoder.encode("password123"),
            "기존 이름",
            "01012345678",
            MemberRole.ROLE_USER
        ));
        authorization = "Bearer " + jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void authenticatedMemberCanGetOwnProfile() throws Exception {
        mockMvc.perform(get("/api/members/me")
                .header(HttpHeaders.AUTHORIZATION, authorization))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.memberId").value(member.getId()))
            .andExpect(jsonPath("$.data.email").value("profile@example.com"))
            .andExpect(jsonPath("$.data.name").value("기존 이름"))
            .andExpect(jsonPath("$.data.phone").value("01012345678"));
    }

    @Test
    void unauthenticatedMemberCannotGetProfile() throws Exception {
        mockMvc.perform(get("/api/members/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMemberCanUpdateOwnProfile() throws Exception {
        mockMvc.perform(put("/api/members/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "변경 이름",
                        "phone": "010-9876-5432"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("변경 이름"))
            .andExpect(jsonPath("$.data.phone").value("01098765432"));

        Member updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getName()).isEqualTo("변경 이름");
        assertThat(updatedMember.getPhone()).isEqualTo("01098765432");
    }

    @Test
    void passwordChangeRequiresCurrentPassword() throws Exception {
        mockMvc.perform(patch("/api/members/me/password")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordChangeJson("wrong-password", "newPassword123", "newPassword123")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CURRENT_PASSWORD_MISMATCH"));
    }

    @Test
    void passwordChangeInvalidatesOldPasswordAndAllowsNewPassword() throws Exception {
        mockMvc.perform(patch("/api/members/me/password")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordChangeJson("password123", "newPassword123", "newPassword123")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("password123")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("newPassword123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    private String passwordChangeJson(String currentPassword, String newPassword, String confirmation) {
        return """
            {
                "currentPassword": "%s",
                "newPassword": "%s",
                "newPasswordConfirmation": "%s"
            }
            """.formatted(currentPassword, newPassword, confirmation);
    }

    private String loginJson(String password) {
        return """
            {
                "email": "profile@example.com",
                "password": "%s"
            }
            """.formatted(password);
    }
}
