package com.ymall.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "휴대전화 번호 형식이 올바르지 않습니다.") String phone
) {

    public MemberProfileUpdateRequest {
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.replaceAll("[\\s-]", "");
    }
}
