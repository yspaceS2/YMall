package com.ymall.backend.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.home.dto.HomeMerchandisingResponse;
import com.ymall.backend.home.repository.HomeMerchandisingQueryRepository;
import com.ymall.backend.home.repository.HomeMerchandisingRow;
import com.ymall.backend.home.repository.HomeMerchandisingSection;

@ExtendWith(MockitoExtension.class)
class HomeMerchandisingServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private HomeMerchandisingQueryRepository queryRepository;

    @Test
    void groupsRowsAndUsesKstThirtyDaySalesWindow() {
        when(queryRepository.findMerchandising(org.mockito.ArgumentMatchers.any()))
            .thenReturn(List.of(
                row(
                    HomeMerchandisingSection.CATEGORY_BEST,
                    1L,
                    "패션",
                    "fashion",
                    10L,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 8, 31),
                    12L
                ),
                row(
                    HomeMerchandisingSection.FASHION,
                    2L,
                    "여성패션",
                    "women-fashion",
                    11L,
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30),
                    7L
                ),
                row(
                    HomeMerchandisingSection.NEW_ARRIVAL,
                    null,
                    null,
                    null,
                    12L,
                    null,
                    null,
                    0L
                )
            ));
        HomeMerchandisingService service = new HomeMerchandisingService(
            queryRepository,
            CLOCK
        );

        HomeMerchandisingResponse response = service.getMerchandising();

        assertThat(response.categoryBest()).hasSize(1);
        assertThat(response.categoryBest().get(0).products().get(0).discountPercentage())
            .isEqualByComparingTo("15");
        assertThat(response.fashion()).hasSize(1);
        assertThat(response.fashion().get(0).products().get(0).discountPercentage())
            .isEqualByComparingTo("0");
        assertThat(response.newArrivals()).extracting(product -> product.productId())
            .containsExactly(12L);

        ArgumentCaptor<OffsetDateTime> cutoffCaptor = ArgumentCaptor.forClass(
            OffsetDateTime.class
        );
        verify(queryRepository).findMerchandising(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue().toInstant())
            .isEqualTo(NOW.minusSeconds(30L * 24 * 60 * 60));
        assertThat(cutoffCaptor.getValue().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    private HomeMerchandisingRow row(
        HomeMerchandisingSection section,
        Long groupCategoryId,
        String groupCategoryName,
        String groupCategorySlug,
        Long productId,
        LocalDate discountStartDate,
        LocalDate discountEndDate,
        long salesQuantity
    ) {
        return new HomeMerchandisingRow(
            section,
            groupCategoryId,
            groupCategoryName,
            groupCategorySlug,
            productId,
            3L,
            "여성 상의",
            "테스트 상품 " + productId,
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(15),
            discountStartDate,
            discountEndDate,
            BigDecimal.valueOf(4.5),
            10,
            "/images/product.jpg",
            salesQuantity
        );
    }
}
