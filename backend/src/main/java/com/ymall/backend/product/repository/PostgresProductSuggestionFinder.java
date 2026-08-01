package com.ymall.backend.product.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ymall.backend.product.search.KoreanSearchNormalizer;
import com.ymall.backend.product.search.ProductSearchMatch;
import com.ymall.backend.product.search.ProductSearchMatchType;

@Repository
@Profile("!test")
public class PostgresProductSuggestionFinder implements ProductSuggestionFinder {

    private static final String SUGGESTION_QUERY = """
        WITH product_sales AS (
            SELECT item.product_id,
                   SUM(GREATEST(item.quantity - item.refunded_quantity, 0)) AS sales_quantity
            FROM order_items item
            JOIN payments payment ON payment.order_id = item.order_id
            WHERE payment.result = 'SUCCESS'
            GROUP BY item.product_id
        ),
        ranked AS (
            SELECT product.id AS product_id,
                   product.name,
                   product.thumbnail_url,
                   COALESCE(sales.sales_quantity, 0) AS sales_quantity,
                   CASE
                       WHEN product.search_normalized_name = :keyword THEN 0
                       WHEN product.search_normalized_name LIKE :prefixPattern THEN 1
                       WHEN product.search_normalized_name LIKE :containsPattern THEN 2
                       WHEN :choseongSearch = TRUE
                           AND product.search_chosung LIKE :containsPattern THEN 3
                       ELSE 4
                   END AS match_rank,
                   similarity(product.search_normalized_name, :keyword) AS similarity_score
            FROM products product
            LEFT JOIN product_sales sales ON sales.product_id = product.id
            WHERE product.status = 'APPROVED'
              AND (:filterCategory = FALSE OR product.category_id IN (:categoryIds))
              AND (
                  product.search_normalized_name LIKE :containsPattern
                  OR (
                      :choseongSearch = TRUE
                      AND product.search_chosung LIKE :containsPattern
                  )
                  OR product.search_normalized_name % :keyword
              )
        )
        SELECT product_id,
               name,
               thumbnail_url,
               sales_quantity,
               match_rank,
               similarity_score
        FROM ranked
        ORDER BY match_rank,
                 similarity_score DESC,
                 sales_quantity DESC,
                 product_id DESC
        LIMIT :limit
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PostgresProductSuggestionFinder(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProductSearchMatch> findMatches(
        String normalizedKeyword,
        Set<Long> categoryIds,
        int limit
    ) {
        boolean choseongSearch = KoreanSearchNormalizer.isChoseongQuery(normalizedKeyword);
        boolean filterCategory = !categoryIds.isEmpty();
        return jdbcTemplate.query(
            SUGGESTION_QUERY,
            Map.of(
                "keyword", normalizedKeyword,
                "prefixPattern", normalizedKeyword + "%",
                "containsPattern", "%" + normalizedKeyword + "%",
                "choseongSearch", choseongSearch,
                "filterCategory", filterCategory,
                "categoryIds", filterCategory ? categoryIds : Set.of(-1L),
                "limit", limit
            ),
            (resultSet, rowNumber) -> new ProductSearchMatch(
                resultSet.getLong("product_id"),
                resultSet.getString("name"),
                resultSet.getString("thumbnail_url"),
                matchType(resultSet.getInt("match_rank")),
                resultSet.getDouble("similarity_score"),
                resultSet.getLong("sales_quantity")
            )
        );
    }

    private ProductSearchMatchType matchType(int rank) {
        return switch (rank) {
            case 0 -> ProductSearchMatchType.EXACT;
            case 1 -> ProductSearchMatchType.PREFIX;
            case 2 -> ProductSearchMatchType.CONTAINS;
            case 3 -> ProductSearchMatchType.CHOSEONG;
            default -> ProductSearchMatchType.FUZZY;
        };
    }
}
