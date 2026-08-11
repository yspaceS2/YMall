package com.ymall.backend.global.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final OAuthFlowContext oAuthFlowContext;

    @Value("${ymall.oauth2.frontend-redirect-uri:http://localhost:5173/oauth2/callback}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauthException
            ? oauthException.getError().getErrorCode()
            : "unknown";
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        log.warn(
            "OAuth2 authentication failed: errorType={}, errorCode={}, causeType={}",
            exception.getClass().getSimpleName(),
            errorCode,
            rootCause.getClass().getSimpleName()
        );
        boolean emailChangeReauthentication =
            oAuthFlowContext.consumeEmailChangeReauthenticationFailure();
        String message = URLEncoder.encode(
            emailChangeReauthentication
                ? "연결된 동일한 소셜 계정으로 다시 로그인해 주세요."
                : "소셜 로그인에 실패했습니다.",
            StandardCharsets.UTF_8
        );
        response.sendRedirect(
            frontendRedirectUri
                + (emailChangeReauthentication
                    ? "#emailChangeReauthenticationError="
                    : "#error=")
                + message
        );
    }
}
