package com.ymall.backend.seller.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.seller.dto.SellerApplicationResponse;
import com.ymall.backend.seller.dto.SellerApplicationReviewRequest;
import com.ymall.backend.seller.entity.SellerApplicationStatus;
import com.ymall.backend.seller.service.SellerApplicationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/seller-applications")
public class AdminSellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    @GetMapping
    public ApiResponse<PageResponse<SellerApplicationResponse>> getApplications(
        @RequestParam(defaultValue = "PENDING") SellerApplicationStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(
            sellerApplicationService.getApplications(status, page, size, keyword)
        );
    }

    @GetMapping("/{sellerApplicationId}")
    public ApiResponse<SellerApplicationResponse> getApplication(
        @PathVariable Long sellerApplicationId
    ) {
        return ApiResponse.success(
            sellerApplicationService.getApplication(sellerApplicationId)
        );
    }

    @PatchMapping("/{sellerApplicationId}")
    public ApiResponse<SellerApplicationResponse> review(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long sellerApplicationId,
        @Valid @RequestBody SellerApplicationReviewRequest request
    ) {
        return ApiResponse.success(
            sellerApplicationService.review(
                principal.memberId(),
                sellerApplicationId,
                request
            ),
            request.status() == SellerApplicationStatus.APPROVED
                ? "판매자 신청을 승인했습니다."
                : request.status() == SellerApplicationStatus.NEEDS_REVISION
                    ? "판매자 신청의 보완을 요청했습니다."
                    : "판매자 신청을 반려했습니다."
        );
    }
}
