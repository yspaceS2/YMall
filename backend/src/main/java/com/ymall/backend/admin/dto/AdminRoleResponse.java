package com.ymall.backend.admin.dto;

import java.util.Set;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

public record AdminRoleResponse(
    Long memberId,
    MemberRole role,
    AdminGrade adminGrade,
    Set<AdminPermission> permissions
) {
    public static AdminRoleResponse from(Member member) {
        AdminGrade adminGrade = member.getAdminGrade();
        return new AdminRoleResponse(
            member.getId(),
            member.getRole(),
            adminGrade,
            adminGrade == null ? Set.of() : adminGrade.permissions()
        );
    }
}
