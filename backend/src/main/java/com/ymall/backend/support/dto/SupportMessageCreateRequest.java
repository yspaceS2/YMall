package com.ymall.backend.support.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupportMessageCreateRequest(
    @NotNull UUID clientMessageId,
    @NotBlank @Size(max = 2000) String content
) {
}
