package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Size;

public record EmailChangeReauthenticationRequest(
    @Size(max = 64)
    String currentPassword
) {
}
