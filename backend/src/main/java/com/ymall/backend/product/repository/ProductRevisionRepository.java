package com.ymall.backend.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.product.entity.ProductRevision;
import com.ymall.backend.product.entity.ProductStatus;

public interface ProductRevisionRepository extends JpaRepository<ProductRevision, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.sellerProfile",
        "category"})
    Optional<ProductRevision> findByProductIdAndStatus(Long productId, ProductStatus status);

    @EntityGraph(attributePaths = {"product", "product.category", "product.sellerProfile",
        "category"})
    @Query("select revision from ProductRevision revision where revision.id = :revisionId")
    Optional<ProductRevision> findDetailById(@Param("revisionId") Long revisionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "product.category", "product.sellerProfile",
        "category"})
    @Query("select revision from ProductRevision revision where revision.id = :revisionId")
    Optional<ProductRevision> findByIdForReview(@Param("revisionId") Long revisionId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.sellerProfile",
        "category"})
    @Query("""
        select revision from ProductRevision revision
        where revision.status = :status
          and (
            :keyword = ''
            or lower(revision.name) like lower(concat('%', :keyword, '%'))
            or lower(revision.brand) like lower(concat('%', :keyword, '%'))
            or lower(revision.product.sellerProfile.storeName) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<ProductRevision> search(
        @Param("status") ProductStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
