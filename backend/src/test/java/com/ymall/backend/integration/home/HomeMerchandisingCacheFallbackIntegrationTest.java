package com.ymall.backend.integration.home;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.home.dto.HomeMerchandisingResponse;
import com.ymall.backend.home.service.HomeMerchandisingService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest(properties = {
    "spring.data.redis.port=1",
    "spring.data.redis.connect-timeout=100ms",
    "spring.data.redis.timeout=100ms"
})
@ActiveProfiles("test")
@Transactional
class HomeMerchandisingCacheFallbackIntegrationTest {

    @Autowired private HomeMerchandisingService merchandisingService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    @Test
    void loadsMerchandisingFromDatabaseWhenRedisIsUnavailable() {
        Category category = categoryRepository.save(
            new Category("생활", "living", null, 1, 1, true)
        );
        Product product = productRepository.save(new Product(
            category,
            "Redis 장애 대체 상품",
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "/images/product.jpg",
            ProductStatus.APPROVED
        ));

        HomeMerchandisingResponse response = merchandisingService.getMerchandising();

        assertThat(response.categoryBest()).hasSize(1);
        assertThat(response.categoryBest().get(0).products())
            .extracting(item -> item.productId())
            .containsExactly(product.getId());
    }
}
