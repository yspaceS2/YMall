package com.ymall.backend.settlement.controller;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.settlement.dto.SettlementLedgerResponse;
import com.ymall.backend.settlement.entity.SettlementStatus;
import com.ymall.backend.settlement.service.SettlementLedgerService;

@RestController
@RequestMapping("/api/seller/settlements")
@RequiredArgsConstructor
public class SellerSettlementLedgerController {

    private final SettlementLedgerService ledgerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SettlementLedgerResponse>>> getLedger(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(required = false) SettlementStatus status,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getSellerLedger(
            principal.memberId(),
            status,
            from,
            to,
            page,
            size
        )));
    }
}
