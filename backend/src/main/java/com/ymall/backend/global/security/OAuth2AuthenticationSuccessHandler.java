package com.ymall.backend.global.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.member.dto.TokenResponse;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthFlowContext oAuthFlowContext;

    @Value("${ymall.oauth2.frontend-redirect-uri:http://localhost:5173/oauth2/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        YMallOAuthPrincipal principal = (YMallOAuthPrincipal) authentication.getPrincipal();
        if (principal.member() == null) {
            oAuthFlowContext.start(request, principal.provider(), principal.profile());
            response.sendRedirect(frontendRedirectUri + "#signupRequired=true");
            return;
        }
        TokenResponse token = jwtTokenProvider.createAccessToken(principal.member());
        String encodedToken = URLEncoder.encode(token.accessToken(), StandardCharsets.UTF_8);
        response.sendRedirect(frontendRedirectUri + "#accessToken=" + encodedToken);
    }
}
