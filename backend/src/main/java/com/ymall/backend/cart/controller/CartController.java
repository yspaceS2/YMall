package com.ymall.backend.cart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.cart.dto.CartItemAddRequest;
import com.ymall.backend.cart.dto.CartItemQuantityUpdateRequest;
import com.ymall.backend.cart.dto.CartItemResponse;
import com.ymall.backend.cart.dto.CartResponse;
import com.ymall.backend.cart.service.CartService;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.success(cartService.getCart(principal.memberId()));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody CartItemAddRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(cartService.addItem(principal.memberId(), request),
                "장바구니에 상품을 담았습니다."));
    }

    @PatchMapping("/items/{cartItemId}")
    public ApiResponse<CartItemResponse> updateQuantity(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long cartItemId,
        @Valid @RequestBody CartItemQuantityUpdateRequest request
    ) {
        return ApiResponse.success(
            cartService.updateQuantity(principal.memberId(), cartItemId, request),
            "장바구니 수량을 변경했습니다."
        );
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteItem(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long cartItemId
    ) {
        cartService.deleteItem(principal.memberId(), cartItemId);
        return ResponseEntity.noContent().build();
    }
}
