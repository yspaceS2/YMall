package com.ymall.backend.home.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HomeMerchandisingQueryRepository {

    private static final int NEW_ARRIVAL_LIMIT = 8;

    private static final String MERCHANDISING_QUERY = """
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
                   END AS root_name,
                   CASE
                       WHEN leaf.depth = 1 THEN leaf.slug
                       WHEN leaf.depth = 2 THEN parent.slug
                       ELSE grandparent.slug
                   END AS root_slug,
                   CASE
                       WHEN leaf.depth = 1 THEN leaf.display_order
                       WHEN leaf.depth = 2 THEN parent.display_order
                       ELSE grandparent.display_order
                   END AS root_order,
                   CASE
                       WHEN leaf.depth = 2 THEN leaf.id
                       WHEN leaf.depth = 3 THEN parent.id
                       ELSE CAST(NULL AS BIGINT)
                   END AS level_two_id,
                   CASE
                       WHEN leaf.depth = 2 THEN leaf.name
                       WHEN leaf.depth = 3 THEN parent.name
                       ELSE CAST(NULL AS VARCHAR(100))
                   END AS level_two_name,
                   CASE
                       WHEN leaf.depth = 2 THEN leaf.slug
                       WHEN leaf.depth = 3 THEN parent.slug
                       ELSE CAST(NULL AS VARCHAR(100))
                   END AS level_two_slug,
                   CASE
                       WHEN leaf.depth = 2 THEN leaf.display_order
                       WHEN leaf.depth = 3 THEN parent.display_order
                       ELSE CAST(NULL AS INTEGER)
                   END AS level_two_order
            FROM categories leaf
            LEFT JOIN categories parent ON parent.id = leaf.parent_id
            LEFT JOIN categories grandparent ON grandparent.id = parent.parent_id
            WHERE leaf.active = TRUE
              AND (parent.id IS NULL OR parent.active = TRUE)
              AND (grandparent.id IS NULL OR grandparent.active = TRUE)
        ),
        paid_orders AS (
            SELECT payment.order_id, MAX(payment.approved_at) AS sold_at
            FROM payments payment
            WHERE payment.result = 'SUCCESS'
              AND payment.approved_at >= :soldAfter
            GROUP BY payment.order_id
        ),
        product_sales AS (
            SELECT item.product_id,
                   SUM(GREATEST(item.quantity - item.refunded_quantity, 0)) AS sales_quantity,
                   MAX(paid_order.sold_at) AS last_sold_at
            FROM order_items item
            JOIN paid_orders paid_order ON paid_order.order_id = item.order_id
            GROUP BY item.product_id
        ),
        review_counts AS (
            SELECT review.product_id,
                   COUNT(*) AS review_count
            FROM reviews review
            GROUP BY review.product_id
        ),
        eligible AS (
            SELECT product.id AS product_id,
                   product.category_id,
                   category.name AS category_name,
                   product.name AS product_name,
                   product.brand,
                   product.price,
                   product.discount_percentage,
                   product.discount_start_date,
                   product.discount_end_date,
                   product.rating,
                   COALESCE(review_count.review_count, 0) AS review_count,
                   product.thumbnail_url,
                   COALESCE(product.approved_at, product.updated_at) AS approved_at,
                   COALESCE(sales.sales_quantity, 0) AS sales_quantity,
                   sales.last_sold_at,
                   tree.root_id,
                   tree.root_name,
                   tree.root_slug,
                   tree.root_order,
                   tree.level_two_id,
                   tree.level_two_name,
                   tree.level_two_slug,
                   tree.level_two_order
            FROM products product
            JOIN categories category ON category.id = product.category_id
            JOIN category_tree tree ON tree.category_id = product.category_id
            LEFT JOIN product_sales sales ON sales.product_id = product.id
            LEFT JOIN review_counts review_count ON review_count.product_id = product.id
            WHERE product.status = 'APPROVED'
              AND product.stock > 0
        ),
        category_ranked AS (
            SELECT eligible.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY root_id
                       ORDER BY sales_quantity DESC,
                                last_sold_at DESC NULLS LAST,
                                approved_at DESC,
                                product_id DESC
                   ) AS product_rank
            FROM eligible
        ),
        grocery_ranked AS (
            SELECT eligible.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY level_two_id
                       ORDER BY sales_quantity DESC,
                                last_sold_at DESC NULLS LAST,
                                approved_at DESC,
                                product_id DESC
                   ) AS product_rank
            FROM eligible
            WHERE root_slug = 'food'
              AND level_two_id IS NOT NULL
        ),
        fashion_ranked AS (
            SELECT eligible.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY level_two_id
                       ORDER BY sales_quantity DESC,
                                last_sold_at DESC NULLS LAST,
                                approved_at DESC,
                                product_id DESC
                   ) AS product_rank
            FROM eligible
            WHERE root_slug = 'fashion'
              AND level_two_id IS NOT NULL
        ),
        new_arrival_ranked AS (
            SELECT eligible.*,
                   ROW_NUMBER() OVER (
                       ORDER BY approved_at DESC, product_id DESC
                   ) AS product_rank
            FROM eligible
        ),
        selected AS (
            SELECT 1 AS section_order,
                   'CATEGORY_BEST' AS section_type,
                   root_id AS group_category_id,
                   root_name AS group_category_name,
                   root_slug AS group_category_slug,
                   root_order AS group_order,
                   category_ranked.*
            FROM category_ranked
            WHERE product_rank = 1
            UNION ALL
            SELECT 2,
                   'GROCERY',
                   level_two_id,
                   level_two_name,
                   level_two_slug,
                   level_two_order,
                   grocery_ranked.*
            FROM grocery_ranked
            WHERE product_rank <= 2
            UNION ALL
            SELECT 3,
                   'FASHION',
                   level_two_id,
                   level_two_name,
                   level_two_slug,
                   level_two_order,
                   fashion_ranked.*
            FROM fashion_ranked
            WHERE product_rank <= 2
            UNION ALL
            SELECT 4,
                   'NEW_ARRIVAL',
                   CAST(NULL AS BIGINT),
                   CAST(NULL AS VARCHAR(100)),
                   CAST(NULL AS VARCHAR(100)),
                   0,
                   new_arrival_ranked.*
            FROM new_arrival_ranked
            WHERE product_rank <= :newArrivalLimit
        )
        SELECT section_type,
               group_category_id,
               group_category_name,
               group_category_slug,
               product_id,
               category_id,
               category_name,
               product_name,
               brand,
               price,
               discount_percentage,
               discount_start_date,
               discount_end_date,
               rating,
               review_count,
               thumbnail_url,
               sales_quantity
        FROM selected
        ORDER BY section_order, group_order, product_rank
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public HomeMerchandisingQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<HomeMerchandisingRow> findMerchandising(OffsetDateTime soldAfter) {
        return jdbcTemplate.query(
            MERCHANDISING_QUERY,
            Map.of(
                "soldAfter", soldAfter,
                "newArrivalLimit", NEW_ARRIVAL_LIMIT
            ),
            (resultSet, rowNumber) -> new HomeMerchandisingRow(
                HomeMerchandisingSection.valueOf(resultSet.getString("section_type")),
                resultSet.getObject("group_category_id", Long.class),
                resultSet.getString("group_category_name"),
                resultSet.getString("group_category_slug"),
                resultSet.getLong("product_id"),
                resultSet.getLong("category_id"),
                resultSet.getString("category_name"),
                resultSet.getString("product_name"),
                resultSet.getString("brand"),
                resultSet.getBigDecimal("price"),
                resultSet.getBigDecimal("discount_percentage"),
                toLocalDate(resultSet.getDate("discount_start_date")),
                toLocalDate(resultSet.getDate("discount_end_date")),
                resultSet.getBigDecimal("rating"),
                resultSet.getLong("review_count"),
                resultSet.getString("thumbnail_url"),
                resultSet.getLong("sales_quantity")
            )
        );
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
