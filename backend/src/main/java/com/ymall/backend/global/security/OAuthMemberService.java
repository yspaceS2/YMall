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

    @Transactional(readOnly = true)
    public Member resolveEmailChangeReauthentication(
        OAuthProvider provider,
        OAuth2UserProfile profile,
        Long expectedMemberId
    ) {
        Member member = oAuthAccountRepository
            .findByProviderAndProviderUserId(provider, profile.providerUserId())
            .map(OAuthAccount::getMember)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.EMAIL_CHANGE_OAUTH_ACCOUNT_MISMATCH
            ));
        if (!member.getId().equals(expectedMemberId)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_OAUTH_ACCOUNT_MISMATCH);
        }
        return member;
    }

    /**
     * 공급자와 공급자 사용자 ID를 계정의 유일한 외부 식별자로 사용해 로그인·연결을 해석한다.
     *
     * <p>이메일이 같다는 이유만으로 기존 회원과 자동 병합하지 않는다. 로그인된 회원이 명시적으로
     * 연결을 요청한 경우에만 계정을 연결하며, Kakao에서 필수 가입 정보가 부족한 경우에는 별도
     * 가입 완료 절차를 요구한다.</p>
     */
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
