package com.ymall.backend.order.returnrequest.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestResponse;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestReviewRequest;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.order.returnrequest.service.ReturnRequestService;

@RestController
@RequestMapping("/api/seller/return-requests")
@RequiredArgsConstructor
public class SellerReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @GetMapping
    public ApiResponse<PageResponse<ReturnRequestResponse>> getRequests(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) ReturnRequestStatus status,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(
            returnRequestService.getSellerRequests(
                principal.memberId(),
                page,
                size,
                status,
                keyword
            )
        );
    }

    @GetMapping("/{returnRequestId}")
    public ApiResponse<ReturnRequestResponse> getRequest(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long returnRequestId
    ) {
        return ApiResponse.success(
            returnRequestService.getSellerRequest(
                principal.memberId(),
                returnRequestId
            )
        );
    }

    @PatchMapping("/{returnRequestId}/approval")
    public ApiResponse<ReturnRequestResponse> approve(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long returnRequestId,
        @Valid @RequestBody ReturnRequestReviewRequest request
    ) {
        return ApiResponse.success(
            returnRequestService.approve(principal.memberId(), returnRequestId, request),
            "반품을 승인하고 환불을 처리했습니다."
        );
    }

    @PatchMapping("/{returnRequestId}/rejection")
    public ApiResponse<ReturnRequestResponse> reject(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long returnRequestId,
        @Valid @RequestBody ReturnRequestReviewRequest request
    ) {
        return ApiResponse.success(
            returnRequestService.reject(principal.memberId(), returnRequestId, request),
            "반품 요청을 거절했습니다."
        );
    }
}
