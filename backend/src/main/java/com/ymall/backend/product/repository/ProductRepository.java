package com.ymall.backend.product.repository;

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

    boolean existsByIdAndStatus(Long productId, ProductStatus status);

    boolean existsByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "sellerProfile"})
    @Query("""
        select product from Product product
        where product.status = :status
          and (
              lower(product.name) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(product.brand, '')) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(product.sellerProfile.storeName, ''))
                  like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<Product> searchAdminProducts(
        @Param("status") ProductStatus status,
        @Param("keyword") String keyword,
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
    Page<Product> findByNameContainingIgnoreCaseAndStatus(
        String keyword,
        ProductStatus status,
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
