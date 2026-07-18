package com.ymall.backend.member.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.dto.MemberAddressRequest;
import com.ymall.backend.member.dto.MemberAddressResponse;
import com.ymall.backend.member.service.MemberAddressService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/addresses")
public class MemberAddressController {
    private final MemberAddressService memberAddressService;

    @GetMapping
    public ApiResponse<List<MemberAddressResponse>> getAddresses(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(memberAddressService.getAddresses(principal.memberId()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MemberAddressResponse>> createAddress(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody MemberAddressRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            memberAddressService.createAddress(principal.memberId(), request),
            "배송지가 등록되었습니다."
        ));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<MemberAddressResponse> updateAddress(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long addressId,
        @Valid @RequestBody MemberAddressRequest request
    ) {
        return ApiResponse.success(
            memberAddressService.updateAddress(principal.memberId(), addressId, request),
            "배송지가 수정되었습니다."
        );
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long addressId
    ) {
        memberAddressService.deleteAddress(principal.memberId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
