package com.ymall.backend.dashboard.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.dashboard.dto.AdminDashboardStatisticsResponse;
import com.ymall.backend.dashboard.dto.DashboardPeriodResponse;
import com.ymall.backend.dashboard.dto.DashboardStatusCountResponse;
import com.ymall.backend.dashboard.dto.DashboardTopProductResponse;
import com.ymall.backend.dashboard.dto.DashboardTrendPointResponse;
import com.ymall.backend.dashboard.dto.SellerDashboardStatisticsResponse;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRepository;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRepository.TrendRow;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.seller.service.SellerProfileService;

@Service
@Transactional(readOnly = true)
public class DashboardStatisticsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

    private final DashboardStatisticsQueryRepository queryRepository;
    private final SellerProfileService sellerProfileService;
    private final Clock clock;

    public DashboardStatisticsService(
        DashboardStatisticsQueryRepository queryRepository,
        SellerProfileService sellerProfileService,
        Clock clock
    ) {
        this.queryRepository = queryRepository;
        this.sellerProfileService = sellerProfileService;
        this.clock = clock;
    }

    public SellerDashboardStatisticsResponse getSellerStatistics(
        Long memberId,
        String periodValue
    ) {
        PeriodRange range = periodRange(periodValue);
        Long sellerProfileId = sellerProfileService.getProfileEntity(memberId).getId();
        List<DashboardTrendPointResponse> trend = fillTrend(
            range,
            queryRepository.findSellerTrend(
                sellerProfileId,
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            )
        );
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        queryRepository.findSellerOrderStatusCounts(
            sellerProfileId,
            range.fromDateTime(),
            range.toDateTime()
        ).forEach(row -> statusCounts.put(OrderStatus.valueOf(row.status()), row.count()));
        var settlement = queryRepository.findSellerSettlement(sellerProfileId);
        var pending = queryRepository.findSellerPending(sellerProfileId);

        return new SellerDashboardStatisticsResponse(
            range.toResponse(),
            trend.stream().map(DashboardTrendPointResponse::netSalesAmount)
                .reduce(ZERO_AMOUNT, BigDecimal::add),
            trend.stream().mapToLong(DashboardTrendPointResponse::orderCount).sum(),
            trend.stream().mapToLong(DashboardTrendPointResponse::salesQuantity).sum(),
            trend,
            List.of(OrderStatus.values()).stream()
                .map(status -> new DashboardStatusCountResponse(
                    status.name(),
                    statusCounts.getOrDefault(status, 0L)
                ))
                .toList(),
            queryRepository.findSellerTopProducts(
                sellerProfileId,
                range.fromDateTime(),
                range.toDateTime()
            ).stream().map(row -> new DashboardTopProductResponse(
                row.productId(),
                row.productName(),
                row.salesQuantity(),
                row.netSalesAmount()
            )).toList(),
            new SellerDashboardStatisticsResponse.SettlementSummary(
                settlement.availableAmount(),
                settlement.processingAmount(),
                settlement.completedAmount()
            ),
            new SellerDashboardStatisticsResponse.PendingTaskSummary(
                pending.orders(),
                pending.returns(),
                pending.questions()
            ),
            generatedAt()
        );
    }

    public AdminDashboardStatisticsResponse getAdminStatistics(String periodValue) {
        PeriodRange range = periodRange(periodValue);
        List<DashboardTrendPointResponse> trend = fillTrend(
            range,
            queryRepository.findAdminTransactionTrend(
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            )
        );
        Map<LocalDate, DashboardStatisticsQueryRepository.RegistrationRow> registrations =
            queryRepository.findAdminRegistrationTrend(
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            ).stream().collect(Collectors.toMap(
                DashboardStatisticsQueryRepository.RegistrationRow::bucket,
                Function.identity()
            ));
        var pending = queryRepository.findAdminPending();

        return new AdminDashboardStatisticsResponse(
            range.toResponse(),
            trend.stream().map(DashboardTrendPointResponse::netSalesAmount)
                .reduce(ZERO_AMOUNT, BigDecimal::add),
            trend.stream().mapToLong(DashboardTrendPointResponse::orderCount).sum(),
            trend.stream().mapToLong(DashboardTrendPointResponse::salesQuantity).sum(),
            trend,
            range.buckets().stream().map(bucket -> {
                var row = registrations.get(bucket);
                return new AdminDashboardStatisticsResponse.RegistrationTrendPoint(
                    bucket,
                    row == null ? 0 : row.members(),
                    row == null ? 0 : row.sellers()
                );
            }).toList(),
            queryRepository.findAdminCategorySales(
                range.fromDateTime(),
                range.toDateTime()
            ).stream().map(row -> new AdminDashboardStatisticsResponse.CategorySales(
                row.categoryId(),
                row.categoryName(),
                row.netSalesAmount(),
                row.salesQuantity()
            )).toList(),
            queryRepository.findAdminTopProducts(
                range.fromDateTime(),
                range.toDateTime()
            ).stream().map(row -> new DashboardTopProductResponse(
                row.productId(),
                row.productName(),
                row.salesQuantity(),
                row.netSalesAmount()
            )).toList(),
            new AdminDashboardStatisticsResponse.PendingTaskSummary(
                pending.products(),
                pending.sellers(),
                pending.refunds(),
                pending.returns(),
                pending.settlements(),
                pending.support()
            ),
            generatedAt()
        );
    }

    private PeriodRange periodRange(String value) {
        DashboardPeriod period = DashboardPeriod.from(value);
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        LocalDate from = period.from(today);
        return new PeriodRange(period, from, today, today.plusDays(1));
    }

    private List<DashboardTrendPointResponse> fillTrend(
        PeriodRange range,
        List<TrendRow> rows
    ) {
        Map<LocalDate, TrendRow> rowsByBucket = rows.stream()
            .collect(Collectors.toMap(TrendRow::bucket, Function.identity()));
        return range.buckets().stream().map(bucket -> {
            TrendRow row = rowsByBucket.get(bucket);
            if (row == null) {
                return new DashboardTrendPointResponse(bucket, ZERO_AMOUNT, 0, 0);
            }
            return new DashboardTrendPointResponse(
                bucket,
                row.netSalesAmount(),
                row.orderCount(),
                row.salesQuantity()
            );
        }).toList();
    }

    private OffsetDateTime generatedAt() {
        return OffsetDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);
    }

    private record PeriodRange(
        DashboardPeriod period,
        LocalDate from,
        LocalDate to,
        LocalDate toExclusive
    ) {
        private boolean monthly() {
            return period.interval() == DashboardPeriod.Interval.MONTH;
        }

        private LocalDateTime fromDateTime() {
            return from.atStartOfDay(BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        }

        private LocalDateTime toDateTime() {
            return toExclusive.atStartOfDay(BUSINESS_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
        }

        private DashboardPeriodResponse toResponse() {
            return new DashboardPeriodResponse(
                period.code(),
                from,
                to,
                period.interval().name()
            );
        }

        private List<LocalDate> buckets() {
            List<LocalDate> buckets = new ArrayList<>();
            LocalDate bucket = from;
            while (!bucket.isAfter(to)) {
                buckets.add(bucket);
                bucket = monthly() ? bucket.plusMonths(1) : bucket.plusDays(1);
            }
            return buckets;
        }
    }
}
