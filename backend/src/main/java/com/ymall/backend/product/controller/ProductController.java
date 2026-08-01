package com.ymall.backend.product.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.dto.ProductSuggestionResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.service.ProductService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductListResponse>> getProducts(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getProducts(page, size));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ProductListResponse>> searchProducts(
        @RequestParam @Size(max = 100, message = "검색어는 100자 이하여야 합니다.") String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.searchProducts(keyword, categoryId, page, size));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<ProductSuggestionResponse>> getProductSuggestions(
        @RequestParam @Size(max = 100, message = "검색어는 100자 이하여야 합니다.") String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(defaultValue = "8") int size
    ) {
        return ApiResponse.success(productService.getProductSuggestions(keyword, categoryId, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.createProduct(request), "상품이 등록되었습니다."));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.success(productService.updateProduct(productId, request), "상품이 수정되었습니다.");
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);

        return ResponseEntity.noContent().build();
    }
}
