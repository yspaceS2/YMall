package com.ymall.backend.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.member.dto.SignupEmailVerificationConfirmRequest;
import com.ymall.backend.member.dto.SignupEmailVerificationConfirmResponse;
import com.ymall.backend.member.dto.SignupEmailVerificationRequest;
import com.ymall.backend.member.dto.SignupEmailVerificationResponse;
import com.ymall.backend.member.service.SignupEmailVerificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/signup/email-verifications")
public class SignupEmailVerificationController {

    private final SignupEmailVerificationService verificationService;

    @PostMapping
    public ApiResponse<SignupEmailVerificationResponse> send(
        @Valid @RequestBody SignupEmailVerificationRequest request
    ) {
        return ApiResponse.success(
            verificationService.send(request.email()),
            "회원가입 이메일 인증번호를 발송했습니다."
        );
    }

    @PostMapping("/confirm")
    public ApiResponse<SignupEmailVerificationConfirmResponse> confirm(
        @Valid @RequestBody SignupEmailVerificationConfirmRequest request
    ) {
        return ApiResponse.success(
            verificationService.confirm(request.requestId(), request.email(), request.code()),
            "이메일 인증이 완료되었습니다."
        );
    }
}
