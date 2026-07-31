package com.ymall.backend.productquestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQuestionAnswerRequest(
    @NotBlank
    @Size(max = 2000)
    String content
) {
}
