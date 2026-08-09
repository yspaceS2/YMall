package com.ymall.backend.dashboard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.AdminPendingRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.CategorySalesRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.RegistrationRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.TopProductRow;
import com.ymall.backend.dashboard.repository.DashboardStatisticsQueryRows.TrendRow;

@Repository
public class AdminDashboardStatisticsQueryRepository {

    private static final String PAID_ORDER_FILTER = """
        EXISTS (
            SELECT 1
            FROM payments payment
            WHERE payment.order_id = orders.id
              AND payment.result = 'SUCCESS'
        )
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminDashboardStatisticsQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrendRow> findAdminTransactionTrend(
        LocalDateTime from,
        LocalDateTime to,
        boolean monthly
    ) {
        String bucket = monthly
            ? "CAST(DATE_TRUNC('month', orders.created_at + INTERVAL '9' HOUR) AS DATE)"
            : "CAST(orders.created_at + INTERVAL '9' HOUR AS DATE)";
        String sql = """
            SELECT %s AS bucket,
                   COALESCE(SUM(item.unit_price * GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS net_sales_amount,
                   COUNT(DISTINCT orders.id) AS order_count,
                   COALESCE(SUM(GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS sales_quantity
            FROM orders
            JOIN order_items item ON item.order_id = orders.id
            WHERE orders.created_at >= :from
              AND orders.created_at < :to
              AND %s
            GROUP BY %s
            ORDER BY bucket
            """.formatted(bucket, PAID_ORDER_FILTER, bucket);
        return queryTrend(sql, Map.of("from", from, "to", to));
    }

    public List<RegistrationRow> findAdminRegistrationTrend(
        LocalDateTime from,
        LocalDateTime to,
        boolean monthly
    ) {
        String memberBucket = monthly
            ? "CAST(DATE_TRUNC('month', member.created_at + INTERVAL '9' HOUR) AS DATE)"
            : "CAST(member.created_at + INTERVAL '9' HOUR AS DATE)";
        String sellerBucket = monthly
            ? "CAST(DATE_TRUNC('month', seller.created_at + INTERVAL '9' HOUR) AS DATE)"
            : "CAST(seller.created_at + INTERVAL '9' HOUR AS DATE)";
        String sql = """
            SELECT registrations.bucket,
                   SUM(registrations.members) AS member_count,
                   SUM(registrations.sellers) AS seller_count
            FROM (
                SELECT %s AS bucket, COUNT(*) AS members, 0 AS sellers
                FROM members member
                WHERE member.created_at >= :from AND member.created_at < :to
                GROUP BY %s
                UNION ALL
                SELECT %s AS bucket, 0 AS members, COUNT(*) AS sellers
                FROM seller_profiles seller
                WHERE seller.created_at >= :from AND seller.created_at < :to
                GROUP BY %s
            ) registrations
            GROUP BY registrations.bucket
            ORDER BY registrations.bucket
            """.formatted(memberBucket, memberBucket, sellerBucket, sellerBucket);
        return jdbcTemplate.query(sql, Map.of("from", from, "to", to),
            (resultSet, rowNumber) -> new RegistrationRow(
                resultSet.getObject("bucket", LocalDate.class),
                resultSet.getLong("member_count"),
                resultSet.getLong("seller_count")
            ));
    }

    public List<CategorySalesRow> findAdminCategorySales(LocalDateTime from, LocalDateTime to) {
        String sql = """
            WITH category_tree AS (
                SELECT leaf.id AS category_id,
                       CASE
                           WHEN leaf.depth = 1 THEN leaf.id
                           WHEN leaf.depth = 2 THEN parent.id
                           ELSE grandparent.id
                       END AS root_id,
                       CASE
                           WHEN leaf.depth = 1 THEN leaf.name
                           WHEN leaf.depth = 2 THEN parent.name
                           ELSE grandparent.name
                       END AS root_name
                FROM categories leaf
                LEFT JOIN categories parent ON parent.id = leaf.parent_id
                LEFT JOIN categories grandparent ON grandparent.id = parent.parent_id
            )
            , category_sales AS (
                SELECT tree.root_id,
                       COALESCE(SUM(item.unit_price * GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS net_sales_amount,
                       COALESCE(SUM(GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS sales_quantity
                FROM orders
                JOIN order_items item ON item.order_id = orders.id
                JOIN products product ON product.id = item.product_id
                JOIN category_tree tree ON tree.category_id = product.category_id
                WHERE orders.created_at >= :from
                  AND orders.created_at < :to
                  AND %s
                GROUP BY tree.root_id
            )
            SELECT root.id AS root_id,
                   root.name AS root_name,
                   COALESCE(sales.net_sales_amount, 0) AS net_sales_amount,
                   COALESCE(sales.sales_quantity, 0) AS sales_quantity
            FROM categories root
            LEFT JOIN category_sales sales ON sales.root_id = root.id
            WHERE root.depth = 1
              AND root.active = TRUE
            ORDER BY net_sales_amount DESC, root.display_order, root.id
            """.formatted(PAID_ORDER_FILTER);

        return jdbcTemplate.query(sql, Map.of("from", from, "to", to),
            (resultSet, rowNumber) -> new CategorySalesRow(
                resultSet.getLong("root_id"),
                resultSet.getString("root_name"),
                resultSet.getBigDecimal("net_sales_amount"),
                resultSet.getLong("sales_quantity")
            ));
    }

    public List<TopProductRow> findAdminTopProducts(LocalDateTime from, LocalDateTime to) {
        return findTopProducts("1 = 1", Map.of("from", from, "to", to, "limit", 5));
    }

    public AdminPendingRow findAdminPending() {
        return jdbcTemplate.queryForObject("""
            SELECT (SELECT COUNT(*) FROM products WHERE status = 'PENDING') AS pending_products,
                   (SELECT COUNT(*) FROM seller_applications WHERE status = 'PENDING') AS pending_sellers,
                   (SELECT COUNT(*) FROM payment_refunds WHERE status = 'PENDING') AS pending_refunds,
                   (SELECT COUNT(*) FROM product_return_requests WHERE status = 'REQUESTED') AS pending_returns,
                   (SELECT COUNT(*) FROM settlement_requests
                    WHERE status IN ('REQUESTED', 'APPROVED')) AS pending_settlements,
                   (SELECT COUNT(*) FROM support_inquiries
                    WHERE status IN ('WAITING', 'LIVE_REQUESTED')) AS pending_support
            """, Map.of(),
            (resultSet, rowNumber) -> new AdminPendingRow(
                resultSet.getLong("pending_products"),
                resultSet.getLong("pending_sellers"),
                resultSet.getLong("pending_refunds"),
                resultSet.getLong("pending_returns"),
                resultSet.getLong("pending_settlements"),
                resultSet.getLong("pending_support")
            ));
    }

    private List<TrendRow> queryTrend(String sql, Map<String, ?> parameters) {
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new TrendRow(
            resultSet.getObject("bucket", LocalDate.class),
            resultSet.getBigDecimal("net_sales_amount"),
            resultSet.getLong("order_count"),
            resultSet.getLong("sales_quantity")
        ));
    }

    private List<TopProductRow> findTopProducts(String scope, Map<String, ?> parameters) {
        String sql = """
            SELECT product.id AS product_id,
                   product.name AS product_name,
                   COALESCE(SUM(GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS sales_quantity,
                   COALESCE(SUM(item.unit_price * GREATEST(item.quantity - item.refunded_quantity, 0)), 0) AS net_sales_amount
            FROM orders
            JOIN order_items item ON item.order_id = orders.id
            JOIN products product ON product.id = item.product_id
            WHERE orders.created_at >= :from
              AND orders.created_at < :to
              AND %s
              AND %s
            GROUP BY product.id, product.name
            HAVING SUM(GREATEST(item.quantity - item.refunded_quantity, 0)) > 0
            ORDER BY sales_quantity DESC, net_sales_amount DESC, product.id DESC
            LIMIT :limit
            """.formatted(scope, PAID_ORDER_FILTER);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new TopProductRow(
            resultSet.getLong("product_id"),
            resultSet.getString("product_name"),
            resultSet.getLong("sales_quantity"),
            resultSet.getBigDecimal("net_sales_amount")
        ));
    }

}
