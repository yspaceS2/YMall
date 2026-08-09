package com.ymall.backend.productquestion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.productquestion.dto.ProductQuestionAnswerRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionPendingCountResponse;
import com.ymall.backend.productquestion.dto.ProductQuestionResponse;
import com.ymall.backend.productquestion.entity.ProductQuestionStatus;
import com.ymall.backend.productquestion.service.ProductQuestionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seller/product-questions")
public class SellerProductQuestionController {

    private final ProductQuestionService productQuestionService;

    @GetMapping
    public ApiResponse<PageResponse<ProductQuestionResponse>> getQuestions(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) ProductQuestionStatus status,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(
            productQuestionService.getSellerQuestions(
                principal.memberId(),
                page,
                size,
                status,
                keyword
            )
        );
    }

    @GetMapping("/pending-count")
    public ApiResponse<ProductQuestionPendingCountResponse> getPendingCount(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(
            productQuestionService.getPendingCount(principal.memberId())
        );
    }

    @GetMapping("/{questionId}")
    public ApiResponse<ProductQuestionResponse> getQuestion(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long questionId
    ) {
        return ApiResponse.success(
            productQuestionService.getSellerQuestion(principal.memberId(), questionId)
        );
    }

    @PutMapping("/{questionId}/answer")
    public ApiResponse<ProductQuestionResponse> answer(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long questionId,
        @Valid @RequestBody ProductQuestionAnswerRequest request
    ) {
        return ApiResponse.success(
            productQuestionService.answer(principal.memberId(), questionId, request),
            "상품 문의 답변이 저장되었습니다."
        );
    }
}
