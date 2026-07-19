package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OAuthSignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Pattern(regexp = "^01[016789][0-9]{7,8}$") String phone
) {
}
