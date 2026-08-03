package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

class MemberPrincipalResolverTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<MemberRepository> memberRepositoryProvider = mock(ObjectProvider.class);
    private final MemberPrincipalResolver resolver = new MemberPrincipalResolver(memberRepositoryProvider);

    MemberPrincipalResolverTest() {
        when(memberRepositoryProvider.getObject()).thenReturn(memberRepository);
    }

    @Test
    void resolvesCurrentAdminGradeAndPermissions() {
        Member admin = member(MemberRole.ROLE_ADMIN);
        MemberPrincipal tokenPrincipal = MemberPrincipal.token(
            admin.getId(),
            admin.getEmail(),
            admin.getRole(),
            admin.getAuthVersion()
        );
        when(memberRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        MemberPrincipal resolved = resolver.resolve(tokenPrincipal);

        assertThat(resolved.adminGrade()).isEqualTo(AdminGrade.SUPER_ADMIN);
        assertThat(resolved.permissions()).contains(AdminPermission.ADMIN_ALL_MANAGE);
        assertThat(resolved.authorities())
            .extracting(authority -> authority.getAuthority())
            .contains("ROLE_ADMIN", "ADMIN_ALL_MANAGE");
    }

    @Test
    void rejectsTokenAfterAuthorizationVersionChanges() {
        Member member = member(MemberRole.ROLE_USER);
        MemberPrincipal oldPrincipal = MemberPrincipal.token(
            member.getId(),
            member.getEmail(),
            member.getRole(),
            member.getAuthVersion()
        );
        member.changeAdminRole(MemberRole.ROLE_ADMIN, AdminGrade.MANAGER);
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> resolver.resolve(oldPrincipal))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void roleOnlyAdminPrincipalDoesNotInferDetailedPermissions() {
        MemberPrincipal principal = new MemberPrincipal(
            1L,
            "admin@example.test",
            MemberRole.ROLE_ADMIN
        );

        assertThat(principal.adminGrade()).isNull();
        assertThat(principal.permissions()).isEmpty();
        assertThat(principal.authorities())
            .extracting(authority -> authority.getAuthority())
            .containsExactly("ROLE_ADMIN");
    }

    private Member member(MemberRole role) {
        Member member = new Member("member@example.test", "password", "Member", role);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
