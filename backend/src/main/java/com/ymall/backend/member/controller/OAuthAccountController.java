package com.ymall.backend.member.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.member.dto.OAuthAccountResponse;
import com.ymall.backend.member.dto.OAuthLinkResponse;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.service.OAuthAccountService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/oauth-accounts")
public class OAuthAccountController {

    private final OAuthAccountService oAuthAccountService;
    private final OAuthFlowContext oAuthFlowContext;

    @GetMapping
    public ApiResponse<List<OAuthAccountResponse>> getAccounts(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(oAuthAccountService.getAccounts(principal.memberId()));
    }

    @PostMapping("/{provider}/links")
    public ApiResponse<OAuthLinkResponse> startLink(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable String provider,
        HttpServletRequest request
    ) {
        OAuthProvider oAuthProvider = oAuthAccountService.getProvider(provider);
        oAuthFlowContext.startLink(request, principal.memberId(), oAuthProvider);
        return ApiResponse.success(new OAuthLinkResponse(
            "/oauth2/authorization/" + provider.toLowerCase()
        ));
    }
}
