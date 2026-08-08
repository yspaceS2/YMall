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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.dashboard.dto.AdminDashboardStatisticsResponse;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.dashboard.dto.DashboardPeriodResponse;
import com.ymall.backend.dashboard.dto.DashboardStatusCountResponse;
import com.ymall.backend.dashboard.dto.DashboardTopProductResponse;
import com.ymall.backend.dashboard.dto.DashboardTrendPointResponse;
import com.ymall.backend.dashboard.dto.SellerDashboardStatisticsResponse;
import com.ymall.backend.dashboard.repository.AdminDashboardStatisticsQueryRepository;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.RegistrationRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.TrendRow;
import com.ymall.backend.dashboard.repository.SellerDashboardStatisticsQueryRepository;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.seller.service.SellerProfileService;

@Service
@Transactional(readOnly = true)
public class DashboardStatisticsService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

    private final SellerDashboardStatisticsQueryRepository sellerQueryRepository;
    private final AdminDashboardStatisticsQueryRepository adminQueryRepository;
    private final SellerProfileService sellerProfileService;
    private final Clock clock;

    public DashboardStatisticsService(
        SellerDashboardStatisticsQueryRepository sellerQueryRepository,
        AdminDashboardStatisticsQueryRepository adminQueryRepository,
        SellerProfileService sellerProfileService,
        Clock clock
    ) {
        this.sellerQueryRepository = sellerQueryRepository;
        this.adminQueryRepository = adminQueryRepository;
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
            sellerQueryRepository.findTrend(
                sellerProfileId,
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            )
        );
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        sellerQueryRepository.findOrderStatusCounts(
            sellerProfileId,
            range.fromDateTime(),
            range.toDateTime()
        ).forEach(row -> statusCounts.put(OrderStatus.valueOf(row.status()), row.count()));
        var settlement = sellerQueryRepository.findSettlement(sellerProfileId);
        var pending = sellerQueryRepository.findPending(sellerProfileId);

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
            sellerQueryRepository.findTopProducts(
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

    public AdminDashboardStatisticsResponse getAdminStatistics(
        Set<AdminPermission> permissions,
        String periodValue
    ) {
        PeriodRange range = periodRange(periodValue);
        List<DashboardTrendPointResponse> trend = fillTrend(
            range,
            adminQueryRepository.findAdminTransactionTrend(
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            )
        );
        Map<LocalDate, RegistrationRow> registrations =
            adminQueryRepository.findAdminRegistrationTrend(
                range.fromDateTime(),
                range.toDateTime(),
                range.monthly()
            ).stream().collect(Collectors.toMap(
                RegistrationRow::bucket,
                Function.identity()
            ));
        var pending = adminQueryRepository.findAdminPending();

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
            adminQueryRepository.findAdminCategorySales(
                range.fromDateTime(),
                range.toDateTime()
            ).stream().map(row -> new AdminDashboardStatisticsResponse.CategorySales(
                row.categoryId(),
                row.categoryName(),
                row.netSalesAmount(),
                row.salesQuantity()
            )).toList(),
            adminQueryRepository.findAdminTopProducts(
                range.fromDateTime(),
                range.toDateTime()
            ).stream().map(row -> new DashboardTopProductResponse(
                row.productId(),
                row.productName(),
                row.salesQuantity(),
                row.netSalesAmount()
            )).toList(),
            new AdminDashboardStatisticsResponse.PendingTaskSummary(
                permitted(permissions, AdminPermission.PRODUCT_REVIEW, pending.products()),
                permitted(
                    permissions,
                    AdminPermission.SELLER_APPLICATION_DECIDE,
                    pending.sellers()
                ),
                permitted(permissions, AdminPermission.REFUND_STANDARD, pending.refunds()),
                permitted(permissions, AdminPermission.REFUND_STANDARD, pending.returns()),
                permitted(
                    permissions,
                    AdminPermission.SETTLEMENT_APPROVE,
                    pending.settlements()
                ),
                permitted(permissions, AdminPermission.SUPPORT_REPLY, pending.support())
            ),
            generatedAt()
        );
    }

    private long permitted(
        Set<AdminPermission> permissions,
        AdminPermission permission,
        long value
    ) {
        return permissions.contains(permission) ? value : 0L;
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
