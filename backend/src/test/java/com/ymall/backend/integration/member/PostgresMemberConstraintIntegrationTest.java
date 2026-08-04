package com.ymall.backend.integration.member;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.OAuthAccountRepository;
import com.ymall.backend.testsupport.PostgresIntegrationTestSupport;

@SpringBootTest
@ActiveProfiles("test")
class PostgresMemberConstraintIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Test
    void rejectsDuplicateMemberEmail() {
        memberRepository.saveAndFlush(member("unique-member@example.test", "First"));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(
            member("unique-member@example.test", "Second")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateProviderUserIdentityAcrossMembers() {
        Member first = memberRepository.saveAndFlush(member("oauth-first@example.test", "First"));
        Member second = memberRepository.saveAndFlush(member("oauth-second@example.test", "Second"));
        oAuthAccountRepository.saveAndFlush(new OAuthAccount(
            first,
            OAuthProvider.GOOGLE,
            "shared-provider-user"
        ));

        assertThatThrownBy(() -> oAuthAccountRepository.saveAndFlush(new OAuthAccount(
            second,
            OAuthProvider.GOOGLE,
            "shared-provider-user"
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsMultipleAccountsFromSameProviderForMember() {
        Member member = memberRepository.saveAndFlush(member("oauth-owner@example.test", "Owner"));
        oAuthAccountRepository.saveAndFlush(new OAuthAccount(
            member,
            OAuthProvider.NAVER,
            "first-provider-user"
        ));

        assertThatThrownBy(() -> oAuthAccountRepository.saveAndFlush(new OAuthAccount(
            member,
            OAuthProvider.NAVER,
            "second-provider-user"
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Member member(String email, String name) {
        return new Member(email, "encoded-password", name, MemberRole.ROLE_USER);
    }
}
