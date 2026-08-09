package com.ymall.backend.seller.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.seller.dto.SellerApplicationCreateRequest;
import com.ymall.backend.seller.dto.SellerApplicationResponse;
import com.ymall.backend.seller.service.SellerApplicationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/seller-application")
public class MemberSellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    @GetMapping
    public ApiResponse<SellerApplicationResponse> getMyApplication(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(
            sellerApplicationService.getMyApplication(principal.memberId())
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> apply(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SellerApplicationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            sellerApplicationService.apply(principal.memberId(), request),
            "판매자 신청이 접수되었습니다."
        ));
    }
}
