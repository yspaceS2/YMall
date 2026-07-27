package com.ymall.backend.wishlist.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.wishlist.dto.WishlistProductResponse;
import com.ymall.backend.wishlist.dto.WishlistStatusResponse;
import com.ymall.backend.wishlist.service.WishlistService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<PageResponse<WishlistProductResponse>> getWishlist(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.success(
            wishlistService.getWishlist(principal.memberId(), page, size)
        );
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<WishlistStatusResponse> getStatus(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId
    ) {
        return ApiResponse.success(
            wishlistService.getStatus(principal.memberId(), productId)
        );
    }

    @PostMapping("/products/{productId}")
    public ApiResponse<WishlistStatusResponse> add(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId
    ) {
        return ApiResponse.success(
            wishlistService.add(principal.memberId(), productId),
            "찜 목록에 상품을 추가했습니다."
        );
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> remove(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId
    ) {
        wishlistService.remove(principal.memberId(), productId);
        return ResponseEntity.noContent().build();
    }
}
