package com.ymall.backend.global.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

public record MemberPrincipal(
    Long memberId,
    String email,
    MemberRole role,
    long authVersion,
    AdminGrade adminGrade,
    Set<AdminPermission> permissions
) {
    public MemberPrincipal {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public MemberPrincipal(Long memberId, String email, MemberRole role) {
        this(memberId, email, role, 0L, null, Set.of());
    }

    public static MemberPrincipal token(
        Long memberId,
        String email,
        MemberRole role,
        long authVersion
    ) {
        return new MemberPrincipal(memberId, email, role, authVersion, null, Set.of());
    }

    public static MemberPrincipal from(Member member) {
        AdminGrade adminGrade = member.getAdminGrade();
        return new MemberPrincipal(
            member.getId(),
            member.getEmail(),
            member.getRole(),
            member.getAuthVersion(),
            adminGrade,
            adminGrade == null ? Set.of() : adminGrade.permissions()
        );
    }

    public List<SimpleGrantedAuthority> authorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.name()));
        permissions.stream()
            .map(AdminPermission::name)
            .map(SimpleGrantedAuthority::new)
            .forEach(authorities::add);
        return List.copyOf(authorities);
    }
}
