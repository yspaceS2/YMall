package com.ymall.backend.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.review.dto.ReviewCreateRequest;
import com.ymall.backend.review.dto.ReviewResponse;
import com.ymall.backend.review.dto.ReviewUpdateRequest;
import com.ymall.backend.review.service.ReviewService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getProductReviews(
        @PathVariable Long productId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(reviewService.getProductReviews(productId, page, size));
    }

    @GetMapping("/reviews/me")
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviews(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(reviewService.getMyReviews(principal.memberId(), page, size));
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                reviewService.createReview(principal.memberId(), request),
                "리뷰가 등록되었습니다."
            ));
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long reviewId,
        @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ApiResponse.success(
            reviewService.updateReview(principal.memberId(), reviewId, request),
            "리뷰가 수정되었습니다."
        );
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(principal.memberId(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
