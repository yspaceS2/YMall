package com.ymall.backend.integration.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductService;

@SpringBootTest(properties = {
    "spring.data.redis.port=1",
    "spring.data.redis.connect-timeout=100ms",
    "spring.data.redis.timeout=100ms"
})
@ActiveProfiles("test")
class ProductCacheFallbackIntegrationTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    @Test
    void loadsProductFromDatabaseWhenRedisIsUnavailable() {
        Category category = categoryRepository.save(new Category("장애 대체", "cache-fallback"));
        Product product = productRepository.save(new Product(
            category,
            "Redis 장애 상품",
            "PostgreSQL 대체 조회 검증",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            ProductStatus.APPROVED
        ));

        ProductDetailResponse response = productService.getProduct(product.getId());

        assertThat(response.name()).isEqualTo("Redis 장애 상품");
    }
}
