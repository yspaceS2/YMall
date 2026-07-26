package com.ymall.backend.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberSignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 255) String emailVerificationToken,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank String passwordConfirmation,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "휴대전화 번호 형식이 올바르지 않습니다.") String phone
) {

    public MemberSignupRequest {
        email = email == null ? null : email.trim();
        name = name == null ? null : name.trim();
        phone = phone == null ? null : phone.replaceAll("[\\s-]", "");
    }

    @AssertTrue(message = "비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirmation);
    }
}
