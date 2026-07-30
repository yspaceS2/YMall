package com.ymall.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminCategoryRequest;
import com.ymall.backend.admin.dto.AdminCategoryResponse;
import com.ymall.backend.admin.service.AdminCategoryService;
import com.ymall.backend.global.common.ApiResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ApiResponse<List<AdminCategoryResponse>> getCategories(
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(adminCategoryService.getCategories(keyword));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<AdminCategoryResponse> getCategory(
        @PathVariable Long categoryId
    ) {
        return ApiResponse.success(adminCategoryService.getCategory(categoryId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminCategoryResponse>> createCategory(
        @Valid @RequestBody AdminCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(
                adminCategoryService.createCategory(request),
                "카테고리를 등록했습니다."
            )
        );
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<AdminCategoryResponse> updateCategory(
        @PathVariable Long categoryId,
        @Valid @RequestBody AdminCategoryRequest request
    ) {
        return ApiResponse.success(
            adminCategoryService.updateCategory(categoryId, request),
            "카테고리를 수정했습니다."
        );
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        adminCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
