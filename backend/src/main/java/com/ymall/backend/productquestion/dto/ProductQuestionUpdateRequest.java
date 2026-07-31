package com.ymall.backend.productquestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQuestionUpdateRequest(
    @NotBlank
    @Size(max = 100)
    String title,

    @NotBlank
    @Size(max = 2000)
    String content,

    boolean privateQuestion
) {
}
