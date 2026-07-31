package com.ymall.backend.seller.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.seller.dto.SellerProductResponse;
import com.ymall.backend.seller.dto.SellerProductStockCondition;
import com.ymall.backend.seller.service.SellerProductService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @GetMapping
    public ApiResponse<PageResponse<SellerProductResponse>> getProducts(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) SellerProductStockCondition stockCondition,
        @RequestParam(required = false) @Min(0) Integer stockQuantity
    ) {
        return ApiResponse.success(sellerProductService.getProducts(
            principal.memberId(),
            page,
            size,
            keyword,
            categoryId,
            stockCondition,
            stockQuantity
        ));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId
    ) {
        return ApiResponse.success(sellerProductService.getProduct(principal.memberId(), productId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody ProductCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            sellerProductService.createProduct(principal.memberId(), request),
            "상품이 등록되어 승인을 기다리고 있습니다."
        ));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> updateProduct(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.success(
            sellerProductService.updateProduct(principal.memberId(), productId, request),
            "상품이 수정되어 재승인을 기다리고 있습니다."
        );
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long productId
    ) {
        sellerProductService.deleteProduct(principal.memberId(), productId);
        return ResponseEntity.noContent().build();
    }
}
