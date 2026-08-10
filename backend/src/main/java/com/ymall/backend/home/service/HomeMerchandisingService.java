package com.ymall.backend.home.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.home.config.HomeCacheNames;
import com.ymall.backend.home.dto.HomeMerchandisingGroupResponse;
import com.ymall.backend.home.dto.HomeMerchandisingProductResponse;
import com.ymall.backend.home.dto.HomeMerchandisingResponse;
import com.ymall.backend.home.repository.HomeMerchandisingQueryRepository;
import com.ymall.backend.home.repository.HomeMerchandisingRow;
import com.ymall.backend.home.repository.HomeMerchandisingSection;

@Service
@Transactional(readOnly = true)
public class HomeMerchandisingService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration SALES_WINDOW = Duration.ofDays(30);

    private final HomeMerchandisingQueryRepository queryRepository;
    private final Clock clock;

    public HomeMerchandisingService(
        HomeMerchandisingQueryRepository queryRepository,
        Clock clock
    ) {
        this.queryRepository = queryRepository;
        this.clock = clock;
    }

    @Cacheable(cacheNames = HomeCacheNames.MERCHANDISING, key = "'all'", sync = true)
    public HomeMerchandisingResponse getMerchandising() {
        OffsetDateTime soldAfter = OffsetDateTime.ofInstant(
            clock.instant().minus(SALES_WINDOW),
            BUSINESS_ZONE
        );
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        List<HomeMerchandisingRow> rows = queryRepository.findMerchandising(soldAfter);

        return new HomeMerchandisingResponse(
            toGroups(rows, HomeMerchandisingSection.CATEGORY_BEST, today),
            toGroups(rows, HomeMerchandisingSection.GROCERY, today),
            toGroups(rows, HomeMerchandisingSection.FASHION, today),
            rows.stream()
                .filter(row -> row.section() == HomeMerchandisingSection.NEW_ARRIVAL)
                .map(row -> toProduct(row, today))
                .toList()
        );
    }

    private List<HomeMerchandisingGroupResponse> toGroups(
        List<HomeMerchandisingRow> rows,
        HomeMerchandisingSection section,
        LocalDate today
    ) {
        Map<Long, GroupBuilder> groups = new LinkedHashMap<>();
        rows.stream()
            .filter(row -> row.section() == section)
            .forEach(row -> groups
                .computeIfAbsent(
                    row.groupCategoryId(),
                    ignored -> new GroupBuilder(
                        row.groupCategoryId(),
                        row.groupCategoryName(),
                        row.groupCategorySlug()
                    )
                )
                .products()
                .add(toProduct(row, today)));

        return groups.values().stream()
            .map(GroupBuilder::toResponse)
            .toList();
    }

    private HomeMerchandisingProductResponse toProduct(
        HomeMerchandisingRow row,
        LocalDate today
    ) {
        return new HomeMerchandisingProductResponse(
            row.productId(),
            row.categoryId(),
            row.categoryName(),
            row.productName(),
            row.brand(),
            row.price(),
            effectiveDiscount(row, today),
            row.rating(),
            row.reviewCount(),
            row.thumbnailUrl(),
            row.salesQuantity()
        );
    }

    private BigDecimal effectiveDiscount(HomeMerchandisingRow row, LocalDate today) {
        BigDecimal percentage = row.discountPercentage();
        if (percentage == null || percentage.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (row.discountStartDate() == null
            || row.discountEndDate() == null
            || today.isBefore(row.discountStartDate())
            || today.isAfter(row.discountEndDate())) {
            return BigDecimal.ZERO;
        }
        return percentage;
    }

    private record GroupBuilder(
        Long categoryId,
        String categoryName,
        String categorySlug,
        List<HomeMerchandisingProductResponse> products
    ) {
        private GroupBuilder(Long categoryId, String categoryName, String categorySlug) {
            this(categoryId, categoryName, categorySlug, new ArrayList<>());
        }

        private HomeMerchandisingGroupResponse toResponse() {
            return new HomeMerchandisingGroupResponse(
                categoryId,
                categoryName,
                categorySlug,
                List.copyOf(products)
            );
        }
    }
}
