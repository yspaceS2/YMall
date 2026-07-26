package com.ymall.backend.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank @Size(min = 20, max = 200) String resetToken,
    @NotBlank @Size(min = 8, max = 64) String newPassword,
    @NotBlank String newPasswordConfirmation
) {

    @AssertTrue(message = "새 비밀번호 확인이 일치하지 않습니다.")
    public boolean isNewPasswordConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirmation);
    }
}
