package com.ymall.backend.seller.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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
import com.ymall.backend.seller.dto.SellerOrderResponse;
import com.ymall.backend.seller.dto.SellerOrderStatusUpdateRequest;
import com.ymall.backend.seller.service.SellerOrderService;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SellerOrderResponse>>> getOrders(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            sellerOrderService.getOrders(principal.memberId(), PageRequest.of(page, size))
        ));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> updateStatus(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody SellerOrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            sellerOrderService.updateStatus(principal.memberId(), orderId, request)
        ));
    }
}
