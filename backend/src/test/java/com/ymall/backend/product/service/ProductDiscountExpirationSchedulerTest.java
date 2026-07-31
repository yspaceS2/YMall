package com.ymall.backend.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductDiscountExpirationSchedulerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCacheInvalidator productCacheInvalidator;

    @Test
    @DisplayName("???? ?? ??? ????? ?? ?? ??? ????")
    void expiresDiscountAndEvictsCache() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC);
        Product product = productWithExpiredDiscount();
        setProductId(product, 1L);
        given(productRepository.findByDiscountPercentageGreaterThanAndDiscountEndDateBefore(
            BigDecimal.ZERO,
            LocalDate.of(2026, 7, 30)
        )).willReturn(List.of(product));
        ProductDiscountExpirationScheduler scheduler = new ProductDiscountExpirationScheduler(
            productRepository,
            productCacheInvalidator,
            clock
        );

        scheduler.expireDiscounts();

        assertThat(product.getDiscountPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(product.getDiscountEndDate()).isNull();
        then(productCacheInvalidator).should().evictDetail(1L);
    }

    private Product productWithExpiredDiscount() {
        return new Product(
            null,
            "??? ??",
            "??",
            "YMall",
            BigDecimal.valueOf(10_000),
            BigDecimal.TEN,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 29),
            true,
            BigDecimal.ZERO,
            3,
            null,
            10,
            null,
            ProductStatus.APPROVED
        );
    }

    private void setProductId(Product product, Long productId) {
        try {
            var idField = Product.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, productId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
