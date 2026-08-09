package com.ymall.backend.productquestion.dto;

import java.time.LocalDateTime;

public record ProductQuestionAnswerResponse(
    Long answerId,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
