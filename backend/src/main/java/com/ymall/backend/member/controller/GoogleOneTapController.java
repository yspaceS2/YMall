package com.ymall.backend.member.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.AuthenticationTokens;
import com.ymall.backend.global.security.GoogleOneTapNonceService;
import com.ymall.backend.global.security.GoogleOneTapProperties;
import com.ymall.backend.global.security.GoogleOneTapTokenVerifier;
import com.ymall.backend.global.security.OAuth2UserProfile;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.global.security.OAuthMemberService;
import com.ymall.backend.global.security.RefreshTokenCookieManager;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.dto.GoogleOneTapLoginRequest;
import com.ymall.backend.member.dto.GoogleOneTapLoginResponse;
import com.ymall.backend.member.dto.GoogleOneTapNonceResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.OAuthProvider;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/oauth2/google/one-tap")
public class GoogleOneTapController {

    private final GoogleOneTapNonceService nonceService;
    private final GoogleOneTapProperties properties;
    private final GoogleOneTapTokenVerifier tokenVerifier;
    private final OAuthMemberService oAuthMemberService;
    private final OAuthFlowContext oAuthFlowContext;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/nonces")
    public ApiResponse<GoogleOneTapNonceResponse> issueNonce() {
        return ApiResponse.success(
            new GoogleOneTapNonceResponse(
                properties.getClientId(),
                nonceService.issue(),
                properties.getNonceTtl().toSeconds()
            ),
            "Google One Tap 요청을 준비했습니다."
        );
    }

    @PostMapping
    public ApiResponse<GoogleOneTapLoginResponse> login(
        @Valid @RequestBody GoogleOneTapLoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        OAuth2UserProfile profile = tokenVerifier.verify(request.credential());
        Optional<Member> member = oAuthMemberService.findExistingMember(
            OAuthProvider.GOOGLE,
            profile.providerUserId()
        );
        if (member.isEmpty()) {
            oAuthFlowContext.start(servletRequest, OAuthProvider.GOOGLE, profile);
            return ApiResponse.success(
                GoogleOneTapLoginResponse.requiresSignup(),
                "추가 회원정보 입력이 필요합니다."
            );
        }

        refreshTokenService.revoke(refreshTokenCookieManager.read(servletRequest));
        AuthenticationTokens tokens = refreshTokenService.issueForLogin(member.get());
        refreshTokenCookieManager.write(servletResponse, tokens.refreshToken());
        return ApiResponse.success(
            GoogleOneTapLoginResponse.authenticated(tokens.accessToken()),
            "Google One Tap 로그인이 완료되었습니다."
        );
    }
}
