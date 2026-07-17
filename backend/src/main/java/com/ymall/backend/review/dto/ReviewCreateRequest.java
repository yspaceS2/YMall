package com.ymall.backend.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
    @NotNull Long orderItemId,
    @NotNull @Min(1) @Max(5) Integer rating,
    @NotBlank @Size(max = 2000) String content
) {
}
