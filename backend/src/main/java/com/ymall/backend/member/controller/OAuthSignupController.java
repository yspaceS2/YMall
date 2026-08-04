package com.ymall.backend.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.AuthenticationTokens;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.global.security.OAuthMemberService;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.member.dto.OAuthSignupRequest;
import com.ymall.backend.member.dto.OAuthEmailVerificationConfirmRequest;
import com.ymall.backend.member.dto.OAuthEmailVerificationRequest;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.service.OAuthEmailVerificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/oauth2")
public class OAuthSignupController {

    private final OAuthFlowContext oAuthFlowContext;
    private final OAuthMemberService oAuthMemberService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;
    private final OAuthEmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public ApiResponse<Void> requestEmailVerification(
        @Valid @RequestBody OAuthEmailVerificationRequest request,
        HttpServletRequest servletRequest
    ) {
        emailVerificationService.send(servletRequest, request.email());
        return ApiResponse.success(null, "인증 이메일을 발송했습니다.");
    }

    @PostMapping("/email-verifications/confirm")
    public ApiResponse<Void> confirmEmailVerification(
        @Valid @RequestBody OAuthEmailVerificationConfirmRequest request,
        HttpServletRequest servletRequest
    ) {
        emailVerificationService.confirm(servletRequest, request.email(), request.code());
        return ApiResponse.success(null, "이메일 인증이 완료되었습니다.");
    }

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(
        @Valid @RequestBody OAuthSignupRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        OAuthFlowContext.PendingSignup pending = oAuthFlowContext.get(servletRequest)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        String normalizedEmail = request.email().trim().toLowerCase();
        String verifiedEmail = oAuthFlowContext.getVerifiedEmail(servletRequest, normalizedEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_EMAIL_VERIFICATION_REQUIRED));
        Member member = oAuthMemberService.completeSignup(
            pending.provider(),
            pending.profile(),
            verifiedEmail,
            request.name(),
            request.phone()
        );
        oAuthFlowContext.clear(servletRequest);
        refreshTokenService.revoke(refreshTokenCookieManager.read(servletRequest));
        AuthenticationTokens tokens = refreshTokenService.issueForLogin(member);
        refreshTokenCookieManager.write(servletResponse, tokens.refreshToken());
        return ApiResponse.success(tokens.accessToken(), "소셜 회원가입이 완료되었습니다.");
    }
}
