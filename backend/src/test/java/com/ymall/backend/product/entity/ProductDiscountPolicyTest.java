package com.ymall.backend.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductDiscountPolicyTest {

    @Test
    @DisplayName("?? ?? ???? ??? ???? ????")
    void returnsConfiguredDiscountDuringPeriod() {
        Product product = productWithDiscount(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31)
        );

        assertThat(product.getEffectiveDiscountPercentage(LocalDate.of(2026, 7, 1)))
            .isEqualByComparingTo("10");
        assertThat(product.getEffectiveDiscountPercentage(LocalDate.of(2026, 7, 31)))
            .isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("?? ?? ???? ???? 0?? ????")
    void returnsZeroOutsideDiscountPeriod() {
        Product product = productWithDiscount(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31)
        );

        assertThat(product.getEffectiveDiscountPercentage(LocalDate.of(2026, 6, 30)))
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(product.getEffectiveDiscountPercentage(LocalDate.of(2026, 8, 1)))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("??? ???? ???? ??? ?????")
    void expiresDiscount() {
        Product product = productWithDiscount(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31)
        );

        product.expireDiscount();

        assertThat(product.getDiscountPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(product.getDiscountStartDate()).isNull();
        assertThat(product.getDiscountEndDate()).isNull();
    }

    private Product productWithDiscount(LocalDate startDate, LocalDate endDate) {
        return new Product(
            null,
            "??? ??",
            "??",
            "YMall",
            BigDecimal.valueOf(10_000),
            BigDecimal.TEN,
            startDate,
            endDate,
            true,
            BigDecimal.ZERO,
            3,
            null,
            10,
            null,
            ProductStatus.APPROVED
        );
    }
}
