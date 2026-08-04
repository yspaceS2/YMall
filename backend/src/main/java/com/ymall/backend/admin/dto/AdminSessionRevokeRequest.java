package com.ymall.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSessionRevokeRequest(
    @NotBlank @Size(max = 500) String reason
) {
}
