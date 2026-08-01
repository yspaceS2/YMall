package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.MemberRole;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final SecurityErrorResponseWriter responseWriter =
        mock(SecurityErrorResponseWriter.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        jwtTokenProvider,
        responseWriter
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void downstreamBusinessExceptionIsNotConvertedToAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = authenticatedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BusinessException downstreamException = new BusinessException(
            ErrorCode.SETTLEMENT_REQUEST_NOT_FOUND
        );
        FilterChain filterChain = (ignoredRequest, ignoredResponse) -> {
            throw downstreamException;
        };

        when(jwtTokenProvider.parseAccessToken("valid-token")).thenReturn(new MemberPrincipal(
            1L,
            "seller@example.com",
            MemberRole.ROLE_SELLER
        ));

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
            .isSameAs(downstreamException);
        verifyNoInteractions(responseWriter);
    }

    @Test
    void invalidTokenIsWrittenAsAuthenticationFailure() throws Exception {
        MockHttpServletRequest request = authenticatedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        BusinessException tokenException = new BusinessException(ErrorCode.INVALID_TOKEN);

        when(jwtTokenProvider.parseAccessToken("valid-token")).thenThrow(tokenException);

        filter.doFilter(request, response, filterChain);

        verify(responseWriter).write(response, ErrorCode.INVALID_TOKEN);
        verifyNoInteractions(filterChain);
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        return request;
    }
}
