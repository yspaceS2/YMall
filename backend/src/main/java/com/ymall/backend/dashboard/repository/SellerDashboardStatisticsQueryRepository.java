package com.ymall.backend.dashboard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.SellerPendingRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.SettlementRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.StatusCountRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.TopProductRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.TrendRow;

@Repository
@RequiredArgsConstructor
public class SellerDashboardStatisticsQueryRepository {

    private static final String PAID_ORDER_FILTER = """
        EXISTS (SELECT 1 FROM payments payment WHERE payment.order_id = orders.id AND payment.result = 'SUCCESS')
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<TrendRow> findTrend(Long sellerProfileId, LocalDateTime from, LocalDateTime to, boolean monthly) {
        String bucket = monthly
            ? "CAST(DATE_TRUNC('month', orders.created_at + INTERVAL '9' HOUR) AS DATE)"
            : "CAST(orders.created_at + INTERVAL '9' HOUR AS DATE)";
        String sql = """
            SELECT %s AS bucket,
                   COALESCE(SUM(item.unit_price * GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS net_sales_amount,
                   COUNT(DISTINCT orders.id) AS order_count,
                   COALESCE(SUM(GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS sales_quantity
            FROM orders JOIN order_items item ON item.order_id = orders.id
            JOIN products product ON product.id = item.product_id
            WHERE product.seller_profile_id = :sellerProfileId AND orders.created_at >= :from
              AND orders.created_at < :to AND %s
            GROUP BY %s ORDER BY bucket
            """.formatted(bucket, PAID_ORDER_FILTER, bucket);
        return jdbcTemplate.query(sql, Map.of("sellerProfileId", sellerProfileId, "from", from, "to", to),
            (rs, row) -> new TrendRow(rs.getObject("bucket", LocalDate.class), rs.getBigDecimal("net_sales_amount"), rs.getLong("order_count"), rs.getLong("sales_quantity")));
    }

    public List<StatusCountRow> findOrderStatusCounts(Long sellerProfileId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
            SELECT orders.status, COUNT(DISTINCT orders.id) AS status_count
            FROM orders JOIN order_items item ON item.order_id = orders.id
            JOIN products product ON product.id = item.product_id
            WHERE product.seller_profile_id = :sellerProfileId
              AND orders.created_at >= :from AND orders.created_at < :to
            GROUP BY orders.status ORDER BY orders.status
            """, Map.of("sellerProfileId", sellerProfileId, "from", from, "to", to),
            (rs, row) -> new StatusCountRow(rs.getString("status"), rs.getLong("status_count")));
    }

    public List<TopProductRow> findTopProducts(Long sellerProfileId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
            SELECT product.id AS product_id, product.name AS product_name,
                   COALESCE(SUM(GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS sales_quantity,
                   COALESCE(SUM(item.unit_price * GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS net_sales_amount
            FROM orders JOIN order_items item ON item.order_id = orders.id
            JOIN products product ON product.id = item.product_id
            WHERE product.seller_profile_id = :sellerProfileId AND orders.created_at >= :from
              AND orders.created_at < :to AND %s
            GROUP BY product.id, product.name
            HAVING SUM(GREATEST(item.quantity - item.refunded_quantity, 0)) > 0
            ORDER BY sales_quantity DESC, net_sales_amount DESC, product.id DESC LIMIT 5
            """.formatted(PAID_ORDER_FILTER), Map.of("sellerProfileId", sellerProfileId, "from", from, "to", to),
            (rs, row) -> new TopProductRow(rs.getLong("product_id"), rs.getString("product_name"), rs.getLong("sales_quantity"), rs.getBigDecimal("net_sales_amount")));
    }

    public SettlementRow findSettlement(Long sellerProfileId) {
        return jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(CASE WHEN status = 'AVAILABLE' THEN settlement_amount ELSE 0 END), 0) AS available_amount,
                   COALESCE(SUM(CASE WHEN status = 'REQUESTED' THEN settlement_amount ELSE 0 END), 0) AS processing_amount,
                   COALESCE(SUM(CASE WHEN status = 'PAID' THEN settlement_amount ELSE 0 END), 0) AS completed_amount
            FROM settlement_ledger_entries WHERE seller_profile_id = :sellerProfileId
            """, Map.of("sellerProfileId", sellerProfileId), (rs, row) -> new SettlementRow(rs.getBigDecimal("available_amount"), rs.getBigDecimal("processing_amount"), rs.getBigDecimal("completed_amount")));
    }

    public SellerPendingRow findPending(Long sellerProfileId) {
        return jdbcTemplate.queryForObject("""
            SELECT (SELECT COUNT(DISTINCT item.order_id) FROM order_items item JOIN products product ON product.id = item.product_id JOIN orders orders ON orders.id = item.order_id WHERE product.seller_profile_id = :sellerProfileId AND item.fulfillment_status IN ('PENDING', 'PREPARING') AND item.refunded_quantity < item.quantity AND orders.status IN ('PAID', 'PARTIALLY_REFUNDED', 'PREPARING')) AS pending_orders,
                   (SELECT COUNT(*) FROM product_return_requests request JOIN order_items item ON item.id = request.order_item_id JOIN products product ON product.id = item.product_id WHERE product.seller_profile_id = :sellerProfileId AND request.status = 'REQUESTED') AS pending_returns,
                   (SELECT COUNT(*) FROM product_questions question JOIN products product ON product.id = question.product_id WHERE product.seller_profile_id = :sellerProfileId AND question.status = 'WAITING') AS pending_questions
            """, Map.of("sellerProfileId", sellerProfileId), (rs, row) -> new SellerPendingRow(rs.getLong("pending_orders"), rs.getLong("pending_returns"), rs.getLong("pending_questions")));
    }
}
