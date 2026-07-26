package com.ymall.backend.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.member.dto.EmailChangeCodeConfirmRequest;
import com.ymall.backend.member.dto.EmailChangeReauthenticationRequest;
import com.ymall.backend.member.dto.EmailChangeReauthenticationResponse;
import com.ymall.backend.member.dto.EmailChangeVerificationRequest;
import com.ymall.backend.member.dto.EmailChangeVerificationResponse;
import com.ymall.backend.member.dto.MemberEmailChangeRequest;
import com.ymall.backend.member.service.MemberEmailChangeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/email-change")
public class MemberEmailChangeController {

    private final MemberEmailChangeService emailChangeService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/reauthentications")
    public ApiResponse<EmailChangeReauthenticationResponse> reauthenticate(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody EmailChangeReauthenticationRequest request
    ) {
        return ApiResponse.success(
            emailChangeService.reauthenticate(principal.memberId(), request.currentPassword()),
            "이메일 변경 본인 확인을 진행했습니다."
        );
    }

    @PostMapping("/reauthentications/confirm")
    public ApiResponse<Void> confirmReauthentication(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody EmailChangeCodeConfirmRequest request
    ) {
        emailChangeService.confirmReauthentication(
            principal.memberId(),
            request.requestId(),
            request.code()
        );
        return ApiResponse.success(null, "본인 확인이 완료되었습니다.");
    }

    @PostMapping("/verifications")
    public ApiResponse<EmailChangeVerificationResponse> sendNewEmailVerification(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody EmailChangeVerificationRequest request
    ) {
        return ApiResponse.success(
            emailChangeService.sendNewEmailVerification(principal.memberId(), request.email()),
            "새 이메일로 인증번호를 발송했습니다."
        );
    }

    @PatchMapping
    public ApiResponse<Void> change(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody MemberEmailChangeRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        emailChangeService.change(
            principal.memberId(),
            request.requestId(),
            request.email(),
            request.code()
        );
        refreshTokenCookieManager.clear(servletResponse);
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success(null, "이메일이 변경되었습니다. 새 이메일로 다시 로그인해 주세요.");
    }
}
