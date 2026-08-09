package com.ymall.backend.review.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.review.entity.Review;
import com.ymall.backend.review.service.ReviewSummaryGenerator;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Page<Review> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "product", "orderItem"})
    Optional<Review> findByIdAndMemberId(Long reviewId, Long memberId);

    @Query("select avg(review.rating) from Review review where review.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductId(Long productId);

    @Query("select max(review.updatedAt) from Review review where review.product.id = :productId")
    LocalDateTime findLatestUpdatedAtByProductId(@Param("productId") Long productId);

    @Query("""
        select new com.ymall.backend.review.service.ReviewSummaryGenerator$Input(
            review.rating,
            review.content,
            review.updatedAt
        )
        from Review review
        where review.product.id = :productId
        order by review.updatedAt desc, review.id desc
        """)
    List<ReviewSummaryGenerator.Input> findSummaryInputsByProductId(
        @Param("productId") Long productId,
        Pageable pageable
    );
}
