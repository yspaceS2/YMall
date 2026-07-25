package com.ymall.backend.review.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.review.entity.ReviewSummary;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {

    Optional<ReviewSummary> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
