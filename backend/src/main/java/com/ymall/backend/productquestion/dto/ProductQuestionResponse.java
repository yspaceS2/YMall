package com.ymall.backend.productquestion.dto;

import java.time.LocalDateTime;

import com.ymall.backend.productquestion.entity.ProductQuestionStatus;

public record ProductQuestionResponse(
    Long questionId,
    Long productId,
    String productName,
    String thumbnailUrl,
    String memberName,
    String title,
    String content,
    boolean privateQuestion,
    boolean ownedByRequester,
    boolean contentVisible,
    ProductQuestionStatus status,
    ProductQuestionAnswerResponse answer,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
