package com.ymall.backend.product.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.service.ProductService;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    /**
     * 카테고리 목록 API가 배열 형태의 data를 공통 응답으로 반환하는지 검증한다.
     * 프론트 카테고리 필터는 categoryId와 slug 값을 사용한다.
     */
    @Test
    @DisplayName("카테고리 목록을 조회한다")
    void getCategories() throws Exception {
        given(productService.getCategories())
            .willReturn(List.of(new CategoryResponse(1L, "smartphones", "smartphones")));

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].categoryId").value(1))
            .andExpect(jsonPath("$.data[0].slug").value("smartphones"));
    }

    /**
     * 카테고리별 상품 조회 API가 page와 size query parameter를 서비스에 반영하고,
     * 상품 목록과 페이징 정보를 공통 응답으로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("카테고리별 상품 목록을 조회한다")
    void getProductsByCategory() throws Exception {
        PageResponse<ProductListResponse> response = new PageResponse<>(
            List.of(productListResponse()),
            1,
            20,
            1,
            1,
            false,
            false
        );

        given(productService.getProductsByCategory(1L, 1, 20)).willReturn(response);

        mockMvc.perform(get("/api/categories/{categoryId}/products", 1L)
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].productId").value(1))
            .andExpect(jsonPath("$.data.page").value(1));
    }

    private ProductListResponse productListResponse() {
        return new ProductListResponse(
            1L,
            1L,
            "smartphones",
            "iPhone 15",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.7),
            20,
            "thumbnail",
            ProductStatus.APPROVED
        );
    }
}
