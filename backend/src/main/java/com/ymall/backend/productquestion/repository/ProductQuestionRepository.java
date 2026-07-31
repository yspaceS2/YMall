package com.ymall.backend.productquestion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.productquestion.entity.ProductQuestion;
import com.ymall.backend.productquestion.entity.ProductQuestionStatus;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    @EntityGraph(attributePaths = {
        "product",
        "product.sellerProfile",
        "product.sellerProfile.member",
        "member",
        "answer"
    })
    Page<ProductQuestion> findByProductIdOrderByCreatedAtDesc(
        Long productId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"product", "member", "answer"})
    Optional<ProductQuestion> findByIdAndMemberId(Long questionId, Long memberId);

    @EntityGraph(attributePaths = {"product", "member", "answer"})
    Optional<ProductQuestion> findByIdAndProductSellerProfileId(
        Long questionId,
        Long sellerProfileId
    );

    @EntityGraph(attributePaths = {"product", "member", "answer"})
    @Query("""
        select question
        from ProductQuestion question
        where question.product.sellerProfile.id = :sellerProfileId
          and (:filterByStatus = false or question.status = :status)
          and (
              :keyword = ''
              or lower(question.product.name) like lower(concat('%', :keyword, '%'))
              or lower(question.title) like lower(concat('%', :keyword, '%'))
              or lower(question.member.name) like lower(concat('%', :keyword, '%'))
          )
        order by question.createdAt desc
        """)
    Page<ProductQuestion> searchSellerQuestions(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("filterByStatus") boolean filterByStatus,
        @Param("status") ProductQuestionStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByProductSellerProfileIdAndStatus(
        Long sellerProfileId,
        ProductQuestionStatus status
    );
}
