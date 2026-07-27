package com.ymall.backend.seller.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.seller.dto.SellerSettlementAccountResponse;
import com.ymall.backend.seller.dto.SellerSettlementAccountUpsertRequest;
import com.ymall.backend.seller.service.SellerSettlementAccountService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seller/settlement-account")
public class SellerSettlementAccountController {

    private final SellerSettlementAccountService settlementAccountService;

    @GetMapping
    public ApiResponse<SellerSettlementAccountResponse> get(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(settlementAccountService.get(principal.memberId()));
    }

    @PutMapping
    public ApiResponse<SellerSettlementAccountResponse> upsert(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SellerSettlementAccountUpsertRequest request
    ) {
        return ApiResponse.success(
            settlementAccountService.upsert(principal.memberId(), request),
            "정산 계좌 정보가 저장되었습니다."
        );
    }
}
