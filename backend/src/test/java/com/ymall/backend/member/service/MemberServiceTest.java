package com.ymall.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.AuthenticationTokens;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.dto.EmailAvailabilityResponse;
import com.ymall.backend.member.dto.MemberLoginRequest;
import com.ymall.backend.member.dto.MemberResponse;
import com.ymall.backend.member.dto.MemberSignupRequest;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SignupEmailVerificationService signupEmailVerificationService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
            memberRepository,
            passwordEncoder,
            refreshTokenService,
            signupEmailVerificationService
        );
    }

    @Test
    void checkEmailAvailabilityReturnsNormalizedEmailResult() {
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);

        EmailAvailabilityResponse response = memberService.checkEmailAvailability(" User@Example.com ");

        assertThat(response.available()).isTrue();
        verify(memberRepository).existsByEmailIgnoreCase("user@example.com");
    }

    @Test
    void signupStoresNormalizedEmailAndEncodedPassword() {
        MemberSignupRequest request = new MemberSignupRequest(
            " User@Example.com ",
            "verification-token",
            "password123",
            "password123",
            "홍길동",
            "010-1234-5678"
        );
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemberResponse response = memberService.signup(request);

        verify(signupEmailVerificationService)
            .consume("verification-token", "user@example.com");
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).saveAndFlush(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
        assertThat(savedMember.getPassword()).isNotEqualTo(request.password());
        assertThat(savedMember.getPhone()).isEqualTo("01012345678");
        assertThat(response.phone()).isEqualTo("01012345678");
        assertThat(response.role()).isEqualTo(MemberRole.ROLE_USER);
    }

    @Test
    void signupRejectsDuplicatedEmail() {
        MemberSignupRequest request = new MemberSignupRequest(
            "user@example.com",
            "verification-token",
            "password123",
            "password123",
            "홍길동",
            "01012345678"
        );
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
    }

    @Test
    void signupMapsConcurrentDuplicateToBusinessException() {
        MemberSignupRequest request = new MemberSignupRequest(
            "user@example.com",
            "verification-token",
            "password123",
            "password123",
            "홍길동",
            "01012345678"
        );
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(memberRepository.saveAndFlush(any(Member.class)))
            .willThrow(new DataIntegrityViolationException("duplicate email"));

        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
    }

    @Test
    void loginReturnsAccessTokenForValidCredentials() {
        Member member = new Member(
            "user@example.com",
            "encoded-password",
            "홍길동",
            MemberRole.ROLE_USER
        );
        MemberLoginRequest request = new MemberLoginRequest("user@example.com", "password123");
        TokenResponse tokenResponse = new TokenResponse("token", "Bearer", 1800);
        given(memberRepository.findByEmailIgnoreCase("user@example.com")).willReturn(java.util.Optional.of(member));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        AuthenticationTokens tokens = new AuthenticationTokens(tokenResponse, "refresh-token");
        given(refreshTokenService.issueForLogin(member)).willReturn(tokens);

        assertThat(memberService.login(request)).isEqualTo(tokens);
    }

    @Test
    void loginRejectsInvalidPassword() {
        Member member = new Member(
            "user@example.com",
            "encoded-password",
            "홍길동",
            MemberRole.ROLE_USER
        );
        MemberLoginRequest request = new MemberLoginRequest("user@example.com", "wrong-password");
        given(memberRepository.findByEmailIgnoreCase("user@example.com")).willReturn(java.util.Optional.of(member));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.LOGIN_FAILED);
    }
}
