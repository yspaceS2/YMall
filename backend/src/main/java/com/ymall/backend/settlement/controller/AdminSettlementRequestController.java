package com.ymall.backend.settlement.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.settlement.dto.SettlementRequestHistoryResponse;
import com.ymall.backend.settlement.dto.SettlementRequestRejectRequest;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.service.SettlementRequestService;

@RestController
@RequestMapping("/api/admin/settlement-requests")
@RequiredArgsConstructor
public class AdminSettlementRequestController {

    private final SettlementRequestService settlementRequestService;

    @GetMapping
    public ApiResponse<PageResponse<SettlementRequestResponse>> getRequests(
        @RequestParam(required = false) SettlementRequestStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
            settlementRequestService.getAdminRequests(status, page, size)
        );
    }

    @PatchMapping("/{settlementRequestId}/approval")
    public ApiResponse<SettlementRequestResponse> approve(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long settlementRequestId
    ) {
        return ApiResponse.success(
            settlementRequestService.approve(principal.memberId(), settlementRequestId),
            "정산 신청을 승인했습니다."
        );
    }

    @PatchMapping("/{settlementRequestId}/rejection")
    public ApiResponse<SettlementRequestResponse> reject(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long settlementRequestId,
        @Valid @RequestBody SettlementRequestRejectRequest request
    ) {
        return ApiResponse.success(
            settlementRequestService.reject(
                principal.memberId(),
                settlementRequestId,
                request.reason()
            ),
            "정산 신청을 반려했습니다."
        );
    }

    @PostMapping("/{settlementRequestId}/mock-payments")
    public ApiResponse<SettlementRequestResponse> completeMockPayment(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long settlementRequestId
    ) {
        return ApiResponse.success(
            settlementRequestService.completeMockPayment(
                principal.memberId(),
                settlementRequestId
            ),
            "모의 지급 처리를 완료했습니다. 실제 계좌 이체는 발생하지 않습니다."
        );
    }

    @GetMapping("/{settlementRequestId}/histories")
    public ApiResponse<List<SettlementRequestHistoryResponse>> getHistory(
        @PathVariable Long settlementRequestId
    ) {
        return ApiResponse.success(settlementRequestService.getHistory(settlementRequestId));
    }
}
