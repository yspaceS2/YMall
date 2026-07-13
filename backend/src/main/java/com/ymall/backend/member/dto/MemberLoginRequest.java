package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MemberLoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {

    public MemberLoginRequest {
        email = email == null ? null : email.trim();
    }
}
