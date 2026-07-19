package com.ymall.backend.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.OAuthMemberService;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.member.dto.OAuthSignupRequest;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/oauth2")
public class OAuthSignupController {

    private final OAuthFlowContext oAuthFlowContext;
    private final OAuthMemberService oAuthMemberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(
        @Valid @RequestBody OAuthSignupRequest request,
        HttpServletRequest servletRequest
    ) {
        OAuthFlowContext.PendingSignup pending = oAuthFlowContext.get(servletRequest)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        Member member = oAuthMemberService.completeSignup(
            pending.provider(),
            pending.profile(),
            request.email(),
            request.name(),
            request.phone()
        );
        oAuthFlowContext.clear(servletRequest);
        return ApiResponse.success(jwtTokenProvider.createAccessToken(member), "소셜 회원가입이 완료되었습니다.");
    }
}
