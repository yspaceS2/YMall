package com.ymall.backend.productquestion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.productquestion.dto.ProductQuestionCreateRequest;
import com.ymall.backend.productquestion.dto.ProductQuestionResponse;
import com.ymall.backend.productquestion.dto.ProductQuestionUpdateRequest;
import com.ymall.backend.productquestion.service.ProductQuestionService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductQuestionController {

    private final ProductQuestionService productQuestionService;

    @GetMapping("/products/{productId}/questions")
    public ApiResponse<PageResponse<ProductQuestionResponse>> getQuestions(
        @PathVariable Long productId,
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(
            productQuestionService.getProductQuestions(productId, principal, page, size)
        );
    }

    @PostMapping("/products/{productId}/questions")
    public ResponseEntity<ApiResponse<ProductQuestionResponse>> create(
        @PathVariable Long productId,
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody ProductQuestionCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(
                productQuestionService.create(principal.memberId(), productId, request),
                "상품 문의가 등록되었습니다."
            )
        );
    }

    @PutMapping("/product-questions/{questionId}")
    public ApiResponse<ProductQuestionResponse> update(
        @PathVariable Long questionId,
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody ProductQuestionUpdateRequest request
    ) {
        return ApiResponse.success(
            productQuestionService.update(principal.memberId(), questionId, request),
            "상품 문의가 수정되었습니다."
        );
    }

    @DeleteMapping("/product-questions/{questionId}")
    public ResponseEntity<Void> delete(
        @PathVariable Long questionId,
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        productQuestionService.delete(principal.memberId(), questionId);
        return ResponseEntity.noContent().build();
    }
}
