package com.ymall.backend.seller.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.seller.dto.SellerProfileCreateRequest;
import com.ymall.backend.seller.dto.SellerProfileResponse;
import com.ymall.backend.seller.dto.SellerProfileUpdateRequest;
import com.ymall.backend.seller.service.SellerProfileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/seller/profile")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @GetMapping
    public ApiResponse<SellerProfileResponse> getProfile(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(sellerProfileService.getProfile(principal.memberId()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerProfileResponse>> createProfile(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SellerProfileCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            sellerProfileService.createProfile(principal.memberId(), request),
            "판매자 프로필이 생성되었습니다."
        ));
    }

    @PutMapping
    public ApiResponse<SellerProfileResponse> updateProfile(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SellerProfileUpdateRequest request
    ) {
        return ApiResponse.success(
            sellerProfileService.updateProfile(principal.memberId(), request),
            "판매자 프로필이 수정되었습니다."
        );
    }
}
