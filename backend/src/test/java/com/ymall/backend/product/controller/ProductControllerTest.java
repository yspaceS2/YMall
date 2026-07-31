package com.ymall.backend.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.service.ProductService;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    /**
     * 상품 목록 API가 공통 응답 래퍼와 페이징 응답 구조를 유지하는지 검증한다.
     * 프론트 목록 화면은 success, data.content, data.page 값을 기준으로 렌더링한다.
     */
    @Test
    @DisplayName("상품 목록을 조회한다")
    void getProducts() throws Exception {
        PageResponse<ProductListResponse> response = new PageResponse<>(
            List.of(productListResponse()),
            1,
            20,
            1,
            1,
            false,
            false
        );

        given(productService.getProducts(1, 20)).willReturn(response);

        mockMvc.perform(get("/api/products")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].productId").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"))
            .andExpect(jsonPath("$.data.page").value(1));
    }

    /**
     * 상품 상세 API가 단건 상품 정보를 공통 응답으로 반환하는지 검증한다.
     * 상세 화면에서 사용하는 category, images, status 필드가 응답에 포함되어야 한다.
     */
    @Test
    @DisplayName("상품 상세를 조회한다")
    void getProduct() throws Exception {
        given(productService.getProduct(1L)).willReturn(productDetailResponse());

        mockMvc.perform(get("/api/products/{productId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.productId").value(1))
            .andExpect(jsonPath("$.data.category.categoryId").value(1))
            .andExpect(jsonPath("$.data.name").value("iPhone 15"))
            .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    /**
     * 상품 생성 API는 유효한 요청 본문을 받으면 201 Created를 반환한다.
     * 생성 응답 메시지는 프론트에서 사용자 피드백으로 사용할 수 있다.
     */
    @Test
    @DisplayName("상품을 등록한다")
    void createProduct() throws Exception {
        given(productService.createProduct(any())).willReturn(productDetailResponse());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productCreateJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("상품이 등록되었습니다."))
            .andExpect(jsonPath("$.data.name").value("iPhone 15"));
    }

    /**
     * @Valid 검증 실패 시 Controller까지 요청이 전달되지 않고 400 응답을 반환하는지 검증한다.
     * 상품명 누락은 ProductCreateRequest의 @NotBlank 정책으로 차단된다.
     */
    @Test
    @DisplayName("상품 등록 요청 값이 올바르지 않으면 400을 반환한다")
    void createProductValidationFail() throws Exception {
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidProductCreateJson()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    /**
     * 상품 수정 API가 productId path variable과 요청 본문을 서비스로 전달하고,
     * 수정 완료 메시지를 공통 응답으로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("상품을 수정한다")
    void updateProduct() throws Exception {
        given(productService.updateProduct(eq(1L), any())).willReturn(productDetailResponse());

        mockMvc.perform(put("/api/products/{productId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productUpdateJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("상품이 수정되었습니다."))
            .andExpect(jsonPath("$.data.productId").value(1));
    }

    /**
     * 상품 삭제 API는 soft delete 처리를 서비스에 위임하고 204 No Content를 반환한다.
     * 응답 본문을 반환하지 않는 계약을 고정한다.
     */
    @Test
    @DisplayName("상품을 삭제한다")
    void deleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/{productId}", 1L))
            .andExpect(status().isNoContent());

        then(productService).should().deleteProduct(1L);
    }

    private String productCreateJson() {
        return """
            {
              "categoryId": 1,
              "name": "iPhone 15",
              "description": "Apple smartphone",
              "brand": "Apple",
              "price": 1200,
              "discountPercentage": 10,
              "discountStartDate": "2026-07-01",
              "discountEndDate": "2026-08-01",
              "stock": 20,
              "thumbnailUrl": "thumbnail",
              "freeShipping": true,
              "shippingFee": 0,
              "estimatedDeliveryDays": 3,
              "images": [
                {
                  "originalUrl": "original",
                  "imageUrl": "image",
                  "sortOrder": 0
                }
              ]
            }
            """;
    }

    private String productUpdateJson() {
        return productCreateJson();
    }

    private String invalidProductCreateJson() {
        return """
            {
              "categoryId": 1,
              "name": "",
              "description": "description",
              "brand": "Apple",
              "price": 1200,
              "discountPercentage": 10,
              "stock": 20,
              "thumbnailUrl": "thumbnail",
              "images": []
            }
            """;
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

    private ProductDetailResponse productDetailResponse() {
        return new ProductDetailResponse(
            1L,
            new CategoryResponse(1L, "smartphones", "smartphones"),
            "iPhone 15",
            "Apple smartphone",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.7),
            20,
            "thumbnail",
            ProductStatus.APPROVED,
            List.of(),
            List.of()
        );
    }
}
