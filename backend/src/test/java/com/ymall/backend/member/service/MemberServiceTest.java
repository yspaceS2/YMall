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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.MemberResponse;
import com.ymall.backend.member.dto.MemberSignupRequest;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, passwordEncoder);
    }

    @Test
    void signupStoresNormalizedEmailAndEncodedPassword() {
        MemberSignupRequest request = new MemberSignupRequest(
            " User@Example.com ",
            "password123",
            "홍길동"
        );
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded-password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        MemberResponse response = memberService.signup(request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(savedMember.getPassword()).isEqualTo("encoded-password");
        assertThat(savedMember.getPassword()).isNotEqualTo(request.password());
        assertThat(response.role()).isEqualTo(MemberRole.ROLE_USER);
    }

    @Test
    void signupRejectsDuplicatedEmail() {
        MemberSignupRequest request = new MemberSignupRequest(
            "user@example.com",
            "password123",
            "홍길동"
        );
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.signup(request))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
    }
}
