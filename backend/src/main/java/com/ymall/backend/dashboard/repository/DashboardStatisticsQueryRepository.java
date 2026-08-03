package com.ymall.backend.dashboard.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardStatisticsQueryRepository {

    private static final String PAID_ORDER_FILTER = """
        EXISTS (
            SELECT 1
            FROM payments payment
            WHERE payment.order_id = orders.id
              AND payment.result = 'SUCCESS'
        )
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DashboardStatisticsQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrendRow> findSellerTrend(
        Long sellerProfileId,
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
            JOIN products product ON product.id = item.product_id
            WHERE product.seller_profile_id = :sellerProfileId
              AND orders.created_at >= :from
              AND orders.created_at < :to
              AND %s
            GROUP BY %s
            ORDER BY bucket
            """.formatted(bucket, PAID_ORDER_FILTER, bucket);
        return queryTrend(sql, Map.of(
            "sellerProfileId", sellerProfileId,
            "from", from,
            "to", to
        ));
    }

    public List<StatusCountRow> findSellerOrderStatusCounts(
        Long sellerProfileId,
        LocalDateTime from,
        LocalDateTime to
    ) {
        return jdbcTemplate.query("""
            SELECT orders.status, COUNT(DISTINCT orders.id) AS status_count
            FROM orders
            JOIN order_items item ON item.order_id = orders.id
            JOIN products product ON product.id = item.product_id
            WHERE product.seller_profile_id = :sellerProfileId
              AND orders.created_at >= :from
              AND orders.created_at < :to
            GROUP BY orders.status
            ORDER BY orders.status
            """, Map.of("sellerProfileId", sellerProfileId, "from", from, "to", to),
            (resultSet, rowNumber) -> new StatusCountRow(
                resultSet.getString("status"),
                resultSet.getLong("status_count")
            ));
    }

    public List<TopProductRow> findSellerTopProducts(
        Long sellerProfileId,
        LocalDateTime from,
        LocalDateTime to
    ) {
        return findTopProducts("product.seller_profile_id = :sellerProfileId", Map.of(
            "sellerProfileId", sellerProfileId,
            "from", from,
            "to", to,
            "limit", 5
        ));
    }

    public SettlementRow findSellerSettlement(Long sellerProfileId) {
        return jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(CASE WHEN status = 'AVAILABLE' THEN settlement_amount ELSE 0 END), 0) AS available_amount,
                   COALESCE(SUM(CASE WHEN status = 'REQUESTED' THEN settlement_amount ELSE 0 END), 0) AS processing_amount,
                   COALESCE(SUM(CASE WHEN status = 'PAID' THEN settlement_amount ELSE 0 END), 0) AS completed_amount
            FROM settlement_ledger_entries
            WHERE seller_profile_id = :sellerProfileId
            """, Map.of("sellerProfileId", sellerProfileId),
            (resultSet, rowNumber) -> new SettlementRow(
                resultSet.getBigDecimal("available_amount"),
                resultSet.getBigDecimal("processing_amount"),
                resultSet.getBigDecimal("completed_amount")
            ));
    }

    public SellerPendingRow findSellerPending(Long sellerProfileId) {
        return jdbcTemplate.queryForObject("""
            SELECT (
                       SELECT COUNT(DISTINCT item.order_id)
                       FROM order_items item
                       JOIN products product ON product.id = item.product_id
                       JOIN orders orders ON orders.id = item.order_id
                       WHERE product.seller_profile_id = :sellerProfileId
                         AND item.fulfillment_status IN ('PENDING', 'PREPARING')
                         AND item.refunded_quantity < item.quantity
                         AND orders.status IN ('PAID', 'PARTIALLY_REFUNDED', 'PREPARING')
                   ) AS pending_orders,
                   (
                       SELECT COUNT(*)
                       FROM product_return_requests return_request
                       JOIN order_items item ON item.id = return_request.order_item_id
                       JOIN products product ON product.id = item.product_id
                       WHERE product.seller_profile_id = :sellerProfileId
                         AND return_request.status = 'REQUESTED'
                   ) AS pending_returns,
                   (
                       SELECT COUNT(*)
                       FROM product_questions question
                       JOIN products product ON product.id = question.product_id
                       WHERE product.seller_profile_id = :sellerProfileId
                         AND question.status = 'WAITING'
                   ) AS pending_questions
            """, Map.of("sellerProfileId", sellerProfileId),
            (resultSet, rowNumber) -> new SellerPendingRow(
                resultSet.getLong("pending_orders"),
                resultSet.getLong("pending_returns"),
                resultSet.getLong("pending_questions")
            ));
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

    public record TrendRow(
        LocalDate bucket,
        BigDecimal netSalesAmount,
        long orderCount,
        long salesQuantity
    ) {
    }

    public record StatusCountRow(String status, long count) {
    }

    public record TopProductRow(
        Long productId,
        String productName,
        long salesQuantity,
        BigDecimal netSalesAmount
    ) {
    }

    public record SettlementRow(
        BigDecimal availableAmount,
        BigDecimal processingAmount,
        BigDecimal completedAmount
    ) {
    }

    public record SellerPendingRow(long orders, long returns, long questions) {
    }

    public record RegistrationRow(LocalDate bucket, long members, long sellers) {
    }

    public record CategorySalesRow(
        Long categoryId,
        String categoryName,
        BigDecimal netSalesAmount,
        long salesQuantity
    ) {
    }

    public record AdminPendingRow(
        long products,
        long sellers,
        long refunds,
        long returns,
        long settlements,
        long support
    ) {
    }
}
