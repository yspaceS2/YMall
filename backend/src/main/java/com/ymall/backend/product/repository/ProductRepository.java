package com.ymall.backend.product.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countBySellerProfileIdAndStatusNot(Long sellerProfileId, ProductStatus status);

    long countBySellerProfileIdAndStatus(Long sellerProfileId, ProductStatus status);

    List<Product> findByDiscountPercentageGreaterThanAndDiscountEndDateBefore(
        BigDecimal discountPercentage,
        LocalDate date
    );

    boolean existsByIdAndStatus(Long productId, ProductStatus status);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    @Query("""
        select product from Product product
        where product.status = :status
          and (
              product.searchNormalizedName like concat('%', :keyword, '%')
              or (:choseongKeyword <> '' and product.searchChosung like concat('%', :choseongKeyword, '%'))
              or lower(replace(coalesce(product.brand, ''), ' ', ''))
                  like lower(concat('%', :keyword, '%'))
              or lower(replace(coalesce(product.sellerProfile.storeName, ''), ' ', ''))
                  like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Product> searchAdminProducts(
        @Param("status") ProductStatus status,
        @Param("keyword") String keyword,
        @Param("choseongKeyword") String choseongKeyword,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryAndStatus(Category category, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryIdInAndStatus(
        Set<Long> categoryIds,
        ProductStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
        select product from Product product
        where product.status = :status
          and (:filterCategory = false or product.category.id in :categoryIds)
          and (
              product.searchNormalizedName like concat('%', :keyword, '%')
              or (:choseongKeyword <> '' and product.searchChosung like concat('%', :choseongKeyword, '%'))
          )
        """)
    Page<Product> searchPublicProducts(
        @Param("keyword") String keyword,
        @Param("choseongKeyword") String choseongKeyword,
        @Param("status") ProductStatus status,
        @Param("filterCategory") boolean filterCategory,
        @Param("categoryIds") Set<Long> categoryIds,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findWithCategoryAndImagesById(Long productId);

    @EntityGraph(attributePaths = "category")
    Page<Product> findBySellerProfileIdAndStatusNot(
        Long sellerProfileId,
        ProductStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    @Query("""
        select product from Product product
        where product.sellerProfile.id = :sellerProfileId
          and product.status <> :excludedStatus
          and (:filterCategory = false or product.category.id in :categoryIds)
          and (:minimumStock is null or product.stock >= :minimumStock)
          and (:maximumStock is null or product.stock <= :maximumStock)
          and (
              :keyword = ''
              or product.searchNormalizedName like concat('%', :keyword, '%')
              or (:choseongKeyword <> '' and product.searchChosung like concat('%', :choseongKeyword, '%'))
              or lower(replace(coalesce(product.brand, ''), ' ', ''))
                  like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Product> searchSellerProducts(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("excludedStatus") ProductStatus excludedStatus,
        @Param("keyword") String keyword,
        @Param("choseongKeyword") String choseongKeyword,
        @Param("filterCategory") boolean filterCategory,
        @Param("categoryIds") Set<Long> categoryIds,
        @Param("minimumStock") Integer minimumStock,
        @Param("maximumStock") Integer maximumStock,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "category")
    List<Product> findTop500ByStatusOrderByUpdatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = "category")
    List<Product> findByIdIn(Collection<Long> productIds);

    @EntityGraph(attributePaths = {"category", "images", "sellerProfile"})
    Optional<Product> findByIdAndSellerProfileIdAndStatusNot(
        Long productId,
        Long sellerProfileId,
        ProductStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    @Query("select product from Product product where product.id = :productId")
    Optional<Product> findByIdForReview(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    @Query("select product from Product product where product.id = :productId")
    Optional<Product> findByIdForAdminView(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :productId")
    Optional<Product> findByIdForRatingUpdate(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :productIds order by product.id")
    List<Product> findAllByIdForUpdate(@Param("productIds") List<Long> productIds);
}
