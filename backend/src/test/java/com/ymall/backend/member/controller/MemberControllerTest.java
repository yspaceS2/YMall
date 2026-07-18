package com.ymall.backend.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.EmailAvailabilityResponse;
import com.ymall.backend.member.dto.MemberResponse;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.service.MemberService;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @Test
    void emailAvailabilityReturnsAvailableResult() throws Exception {
        given(memberService.checkEmailAvailability("user@example.com"))
            .willReturn(new EmailAvailabilityResponse(true));

        mockMvc.perform(get("/api/members/email-availability")
                .param("email", "user@example.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void signupReturnsCreatedMember() throws Exception {
        given(memberService.signup(any())).willReturn(new MemberResponse(
            1L,
            "user@example.com",
            "홍길동",
            "01012345678",
            MemberRole.ROLE_USER,
            LocalDateTime.of(2026, 7, 12, 10, 0)
        ));

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "user@example.com",
                        "password": "password123",
                        "passwordConfirmation": "password123",
                        "name": "홍길동",
                        "phone": "01012345678"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.memberId").value(1))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.phone").value("01012345678"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void signupRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "invalid-email",
                        "password": "short",
                        "passwordConfirmation": "different",
                        "name": "",
                        "phone": "1234"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void signupRejectsMismatchedPasswordConfirmation() throws Exception {
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "user@example.com",
                        "password": "password123",
                        "passwordConfirmation": "different123",
                        "name": "홍길동",
                        "phone": "01012345678"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void signupRejectsDuplicatedEmail() throws Exception {
        given(memberService.signup(any()))
            .willThrow(new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "user@example.com",
                        "password": "password123",
                        "passwordConfirmation": "password123",
                        "name": "홍길동",
                        "phone": "01012345678"
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        given(memberService.login(any()))
            .willReturn(new TokenResponse("access-token", "Bearer", 1800));

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "user@example.com",
                        "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }
}
