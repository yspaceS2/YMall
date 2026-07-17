package com.ymall.backend.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.service.ProductService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(productService.getCategories());
    }

    @GetMapping("/{categoryId}/products")
    public ApiResponse<PageResponse<ProductListResponse>> getProductsByCategory(
        @PathVariable Long categoryId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(productService.getProductsByCategory(categoryId, page, size));
    }
}
