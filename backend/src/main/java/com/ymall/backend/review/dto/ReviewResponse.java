package com.ymall.backend.review.dto;

import java.time.LocalDateTime;

import com.ymall.backend.review.entity.Review;

public record ReviewResponse(
    Long reviewId,
    Long orderItemId,
    Long productId,
    String authorName,
    Integer rating,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getOrderItem().getId(),
            review.getProduct().getId(),
            review.getMember().getName(),
            review.getRating(),
            review.getContent(),
            review.getCreatedAt(),
            review.getUpdatedAt()
        );
    }
}
