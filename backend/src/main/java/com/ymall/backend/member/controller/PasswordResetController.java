package com.ymall.backend.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.dto.PasswordResetConfirmRequest;
import com.ymall.backend.member.dto.PasswordResetRequest;
import com.ymall.backend.member.dto.PasswordResetRequestResponse;
import com.ymall.backend.member.dto.PasswordResetVerificationRequest;
import com.ymall.backend.member.dto.PasswordResetVerificationResponse;
import com.ymall.backend.member.service.PasswordResetService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/password-reset-requests")
    public ApiResponse<PasswordResetRequestResponse> request(
        @Valid @RequestBody PasswordResetRequest request
    ) {
        return ApiResponse.success(
            passwordResetService.request(request.email()),
            "가입된 일반 회원 계정이라면 인증번호를 전송했습니다."
        );
    }

    @PostMapping("/password-reset-verifications")
    public ApiResponse<PasswordResetVerificationResponse> verify(
        @Valid @RequestBody PasswordResetVerificationRequest request
    ) {
        return ApiResponse.success(
            passwordResetService.verify(request.requestId(), request.code()),
            "이메일 인증이 완료되었습니다."
        );
    }

    @PostMapping("/password-resets")
    public ApiResponse<Void> reset(
        @Valid @RequestBody PasswordResetConfirmRequest request,
        HttpServletResponse response
    ) {
        passwordResetService.reset(request);
        refreshTokenCookieManager.clear(response);
        return ApiResponse.success(null, "비밀번호가 재설정되었습니다.");
    }
}
