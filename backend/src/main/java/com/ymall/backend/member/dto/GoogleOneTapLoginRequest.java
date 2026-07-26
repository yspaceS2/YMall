package com.ymall.backend.member.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleOneTapLoginRequest(
    @NotBlank(message = "Google credential은 필수입니다.")
    String credential
) {
}
