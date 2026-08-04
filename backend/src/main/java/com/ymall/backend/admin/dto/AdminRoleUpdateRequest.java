package com.ymall.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.member.entity.MemberRole;

public record AdminRoleUpdateRequest(
    @NotNull MemberRole role,
    AdminGrade adminGrade,
    @NotBlank @Size(max = 500) String reason
) {
}
