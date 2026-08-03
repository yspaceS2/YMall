package com.ymall.backend.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportResolutionRequest(
    @NotBlank @Size(max = 2000) String content
) {
}
