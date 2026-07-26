package com.ymall.backend.global.security;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.OAuthAccountRepository;

@Service
@RequiredArgsConstructor
public class OAuthMemberService {

    private final MemberRepository memberRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    @Transactional(readOnly = true)
    public Optional<Member> findExistingMember(
        OAuthProvider provider,
        String providerUserId
    ) {
        return oAuthAccountRepository
            .findByProviderAndProviderUserId(provider, providerUserId)
            .map(OAuthAccount::getMember);
    }

    @Transactional
    public OAuthLoginResult resolve(
        OAuthProvider provider,
        OAuth2UserProfile profile,
        Long linkMemberId
    ) {
        Member member = oAuthAccountRepository
            .findByProviderAndProviderUserId(provider, profile.providerUserId())
            .map(account -> validateExistingAccount(account, linkMemberId))
            .orElseGet(() -> {
                if (linkMemberId != null) {
                    return linkAccount(linkMemberId, provider, profile.providerUserId());
                }
                return provider == OAuthProvider.KAKAO ? null : registerMember(provider, profile);
            });
        return new OAuthLoginResult(member, member == null);
    }

    @Transactional
    public Member completeSignup(
        OAuthProvider provider,
        OAuth2UserProfile profile,
        String email,
        String name,
        String phone
    ) {
        String normalizedEmail = email.trim().toLowerCase();
        if (memberRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (oAuthAccountRepository.findByProviderAndProviderUserId(
            provider,
            profile.providerUserId()
        ).isPresent()) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }
        Member member = memberRepository.save(new Member(
            normalizedEmail,
            null,
            name.trim(),
            phone,
            MemberRole.ROLE_USER
        ));
        oAuthAccountRepository.save(new OAuthAccount(member, provider, profile.providerUserId()));
        return member;
    }

    private Member validateExistingAccount(OAuthAccount account, Long linkMemberId) {
        if (linkMemberId != null && !account.getMember().getId().equals(linkMemberId)) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }
        return account.getMember();
    }

    private Member linkAccount(Long memberId, OAuthProvider provider, String providerUserId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (oAuthAccountRepository.existsByMemberIdAndProvider(memberId, provider)) {
            throw new BusinessException(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
        }
        oAuthAccountRepository.save(new OAuthAccount(member, provider, providerUserId));
        return member;
    }

    private Member registerMember(OAuthProvider provider, OAuth2UserProfile profile) {
        if (memberRepository.existsByEmailIgnoreCase(profile.email())) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_ALREADY_REGISTERED);
        }
        Member member = memberRepository.save(new Member(
            profile.email(),
            null,
            profile.name(),
            MemberRole.ROLE_USER
        ));
        oAuthAccountRepository.save(new OAuthAccount(member, provider, profile.providerUserId()));
        return member;
    }

    public record OAuthLoginResult(Member member, boolean signupRequired) {
    }
}
