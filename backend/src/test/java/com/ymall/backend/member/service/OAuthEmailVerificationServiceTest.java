package com.ymall.backend.member.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.OAuth2UserProfile;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class OAuthEmailVerificationServiceTest {

    @Mock
    private MemberMailSender memberMailSender;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OAuthFlowContext oAuthFlowContext;
    @Mock
    private HttpServletRequest request;

    private OAuthEmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new OAuthEmailVerificationService(
            memberMailSender,
            memberRepository,
            oAuthFlowContext
        );
    }

    @Test
    void sendsCodeForPendingOAuthSignup() {
        given(oAuthFlowContext.get(request)).willReturn(Optional.of(pendingSignup()));
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);

        service.send(request, " User@Example.com ");

        verify(memberMailSender).send(
            org.mockito.ArgumentMatchers.eq("user@example.com"),
            org.mockito.ArgumentMatchers.eq("[YMall] 이메일 인증번호"),
            org.mockito.ArgumentMatchers.contains("인증번호")
        );
        verify(oAuthFlowContext).startEmailVerification(
            org.mockito.ArgumentMatchers.eq(request),
            org.mockito.ArgumentMatchers.eq("user@example.com"),
            org.mockito.ArgumentMatchers.matches("^[0-9]{6}$")
        );
    }

    @Test
    void rejectsDuplicatedEmail() {
        given(oAuthFlowContext.get(request)).willReturn(Optional.of(pendingSignup()));
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.send(request, "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
    }

    @Test
    void rejectsInvalidVerificationCode() {
        given(oAuthFlowContext.verifyEmail(request, "user@example.com", "123456"))
            .willReturn(false);

        assertThatThrownBy(() -> service.confirm(request, "user@example.com", "123456"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OAUTH_EMAIL_VERIFICATION_FAILED);
    }

    private OAuthFlowContext.PendingSignup pendingSignup() {
        return new OAuthFlowContext.PendingSignup(
            OAuthProvider.KAKAO,
            new OAuth2UserProfile("provider-user", null, "사용자")
        );
    }
}
