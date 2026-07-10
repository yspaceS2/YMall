package com.ymall.backend.integration.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 상품 등록 API가 Controller, Service, Repository, JPA를 모두 통과해
     * 실제 테스트 DB에 상품과 이미지 데이터를 저장하는지 검증한다.
     */
    @Test
    @DisplayName("상품 등록 API는 테스트 DB에 상품을 저장한다")
    void createProduct() throws Exception {
        Category category = categoryRepository.save(new Category("smartphones", "smartphones"));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productCreateJson(category.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("iPhone 15"))
            .andExpect(jsonPath("$.data.images[0].imageUrl").value("image"));

        assertThat(productRepository.findAll()).hasSize(1);
        Product savedProduct = productRepository.findAll().get(0);
        assertThat(savedProduct.getName()).isEqualTo("iPhone 15");
        assertThat(savedProduct.getImages()).hasSize(1);
    }

    /**
     * 상품 목록 API가 실제 DB에 저장된 APPROVED 상품만 조회해
     * 공통 페이징 응답으로 반환하는지 검증한다.
     */
    @Test
    @DisplayName("상품 목록 API는 승인된 상품을 조회한다")
    void getProducts() throws Exception {
        Category category = categoryRepository.save(new Category("smartphones", "smartphones"));
        productRepository.save(createProduct(category, "iPhone 15", ProductStatus.APPROVED));
        productRepository.save(createProduct(category, "Draft Product", ProductStatus.DRAFT));

        mockMvc.perform(get("/api/products")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("iPhone 15"));
    }

    /**
     * 상품 수정 API가 실제 DB에 저장된 상품의 기본 정보와 이미지 목록을
     * 요청 본문 기준으로 교체하는지 검증한다.
     */
    @Test
    @DisplayName("상품 수정 API는 DB 상품 정보를 변경한다")
    void updateProduct() throws Exception {
        Category category = categoryRepository.save(new Category("smartphones", "smartphones"));
        Product product = productRepository.save(createProduct(category, "iPhone 15", ProductStatus.APPROVED));

        mockMvc.perform(put("/api/products/{productId}", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(productUpdateJson(category.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Updated Product"));

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getName()).isEqualTo("Updated Product");
        assertThat(updatedProduct.getImages()).hasSize(1);
    }

    /**
     * 상품 삭제 API가 실제 row를 삭제하지 않고 상태만 DELETED로 변경하는
     * soft delete 정책을 통합 흐름에서 검증한다.
     */
    @Test
    @DisplayName("상품 삭제 API는 상품 상태를 DELETED로 변경한다")
    void deleteProduct() throws Exception {
        Category category = categoryRepository.save(new Category("smartphones", "smartphones"));
        Product product = productRepository.save(createProduct(category, "iPhone 15", ProductStatus.APPROVED));

        mockMvc.perform(delete("/api/products/{productId}", product.getId()))
            .andExpect(status().isNoContent());

        Product deletedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(deletedProduct.getStatus()).isEqualTo(ProductStatus.DELETED);
    }

    private Product createProduct(Category category, String name, ProductStatus status) {
        return new Product(
            category,
            name,
            "description",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.7),
            20,
            "thumbnail",
            status
        );
    }

    private String productCreateJson(Long categoryId) {
        return """
            {
              "categoryId": %d,
              "name": "iPhone 15",
              "description": "Apple smartphone",
              "brand": "Apple",
              "price": 1200,
              "discountPercentage": 10,
              "stock": 20,
              "thumbnailUrl": "thumbnail",
              "images": [
                {
                  "originalUrl": "original",
                  "imageUrl": "image",
                  "sortOrder": 0
                }
              ]
            }
            """.formatted(categoryId);
    }

    private String productUpdateJson(Long categoryId) {
        return """
            {
              "categoryId": %d,
              "name": "Updated Product",
              "description": "Updated description",
              "brand": "Apple",
              "price": 900,
              "discountPercentage": 5,
              "stock": 10,
              "thumbnailUrl": "updated-thumbnail",
              "images": [
                {
                  "originalUrl": "updated-original",
                  "imageUrl": "updated-image",
                  "sortOrder": 0
                }
              ]
            }
            """.formatted(categoryId);
    }
}
