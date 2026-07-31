package com.ymall.backend.settlement.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.ymall.backend.settlement.dto.SettlementAvailabilityResponse;
import com.ymall.backend.settlement.dto.SettlementRequestCreateRequest;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.service.SettlementRequestService;

@RestController
@RequestMapping("/api/seller/settlement-requests")
@RequiredArgsConstructor
public class SellerSettlementRequestController {

    private final SettlementRequestService settlementRequestService;

    @GetMapping("/availability")
    public ApiResponse<SettlementAvailabilityResponse> getAvailability(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(
            settlementRequestService.getAvailability(principal.memberId())
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<SettlementRequestResponse>> getRequests(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(required = false) SettlementRequestStatus status,
        @RequestParam(required = false) Long requestId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate requestedFrom,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate requestedTo,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
            settlementRequestService.getSellerRequests(
                principal.memberId(),
                status,
                requestId,
                requestedFrom,
                requestedTo,
                page,
                size
            )
        );
    }

    @GetMapping("/{settlementRequestId}")
    public ApiResponse<SettlementRequestResponse> getRequest(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long settlementRequestId
    ) {
        return ApiResponse.success(
            settlementRequestService.getSellerRequest(
                principal.memberId(),
                settlementRequestId
            )
        );
    }

    @PostMapping
    public ApiResponse<SettlementRequestResponse> request(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SettlementRequestCreateRequest request
    ) {
        return ApiResponse.success(
            settlementRequestService.request(principal.memberId()),
            "정산을 신청했습니다."
        );
    }
}
