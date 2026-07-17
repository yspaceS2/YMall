package com.ymall.backend.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.admin.service.AdminService;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.entity.ProductStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductResponse>> getProducts(
        @RequestParam(defaultValue = "PENDING") ProductStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.getProducts(status, page, size));
    }

    @PatchMapping("/products/{productId}/status")
    public ApiResponse<AdminProductResponse> updateProductStatus(
        @PathVariable Long productId,
        @Valid @RequestBody AdminProductStatusUpdateRequest request
    ) {
        return ApiResponse.success(
            adminService.updateProductStatus(productId, request),
            "상품 승인 상태를 변경했습니다."
        );
    }

    @GetMapping("/members")
    public ApiResponse<PageResponse<AdminMemberResponse>> getMembers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.getMembers(page, size));
    }

    @GetMapping("/sellers")
    public ApiResponse<PageResponse<AdminSellerResponse>> getSellers(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.getSellers(page, size));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<AdminOrderResponse>> getOrders(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(adminService.getOrders(page, size));
    }
}
