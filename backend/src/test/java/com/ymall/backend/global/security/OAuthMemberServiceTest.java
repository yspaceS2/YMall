package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.OAuthAccountRepository;

@ExtendWith(MockitoExtension.class)
class OAuthMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    private OAuthMemberService service;

    @BeforeEach
    void setUp() {
        service = new OAuthMemberService(memberRepository, oAuthAccountRepository);
    }

    @Test
    void linksNewProviderAccountToAuthenticatedMember() {
        Member member = member(1L, "member@example.com");
        OAuth2UserProfile profile = new OAuth2UserProfile("provider-user", "other@example.com", "사용자");
        given(oAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.KAKAO,
            profile.providerUserId()
        )).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(oAuthAccountRepository.existsByMemberIdAndProvider(1L, OAuthProvider.KAKAO))
            .willReturn(false);

        Member result = service.resolve(OAuthProvider.KAKAO, profile, 1L).member();

        assertThat(result).isSameAs(member);
        ArgumentCaptor<OAuthAccount> captor = ArgumentCaptor.forClass(OAuthAccount.class);
        verify(oAuthAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(captor.getValue().getProviderUserId()).isEqualTo("provider-user");
    }

    @Test
    void rejectsProviderAccountAlreadyLinkedToAnotherMember() {
        Member otherMember = member(2L, "other@example.com");
        OAuthAccount account = new OAuthAccount(otherMember, OAuthProvider.NAVER, "provider-user");
        OAuth2UserProfile profile = new OAuth2UserProfile("provider-user", "member@example.com", "사용자");
        given(oAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.NAVER,
            profile.providerUserId()
        )).willReturn(Optional.of(account));

        assertThatThrownBy(() -> service.resolve(OAuthProvider.NAVER, profile, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void rejectsSecondAccountFromSameProviderForMember() {
        Member member = member(1L, "member@example.com");
        OAuth2UserProfile profile = new OAuth2UserProfile("new-provider-user", "member@example.com", "사용자");
        given(oAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.GOOGLE,
            profile.providerUserId()
        )).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(oAuthAccountRepository.existsByMemberIdAndProvider(1L, OAuthProvider.GOOGLE))
            .willReturn(true);

        assertThatThrownBy(() -> service.resolve(OAuthProvider.GOOGLE, profile, 1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void completesOAuthSignupWithoutPassword() {
        OAuth2UserProfile profile = new OAuth2UserProfile("kakao-user", null, "카카오 사용자");
        given(memberRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(oAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.KAKAO,
            "kakao-user"
        )).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        Member member = service.completeSignup(
            OAuthProvider.KAKAO,
            profile,
            " User@Example.com ",
            "사용자",
            "01012345678"
        );

        assertThat(member.getEmail()).isEqualTo("user@example.com");
        assertThat(member.hasPassword()).isFalse();
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void findsOnlyExistingProviderAccountForOneTapLogin() {
        Member member = member(1L, "member@example.com");
        OAuthAccount account = new OAuthAccount(member, OAuthProvider.GOOGLE, "google-user");
        given(oAuthAccountRepository.findByProviderAndProviderUserId(
            OAuthProvider.GOOGLE,
            "google-user"
        )).willReturn(Optional.of(account));

        Optional<Member> result = service.findExistingMember(
            OAuthProvider.GOOGLE,
            "google-user"
        );

        assertThat(result).containsSame(member);
    }

    private Member member(Long id, String email) {
        Member member = new Member(email, "password", "사용자", MemberRole.ROLE_USER);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
