package com.ymall.backend.review.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.product.entity.Product;

@Getter
@Entity
@Table(
    name = "review_summaries",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_summaries_product_id",
        columnNames = "product_id"
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "summary_json", nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "source_review_count", nullable = false)
    private long sourceReviewCount;

    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "model_version", nullable = false, length = 200)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public ReviewSummary(
        Product product,
        String summaryJson,
        long sourceReviewCount,
        LocalDateTime sourceUpdatedAt,
        String modelVersion,
        LocalDateTime generatedAt
    ) {
        this.product = product;
        update(
            summaryJson,
            sourceReviewCount,
            sourceUpdatedAt,
            modelVersion,
            generatedAt
        );
    }

    public void update(
        String summaryJson,
        long sourceReviewCount,
        LocalDateTime sourceUpdatedAt,
        String modelVersion,
        LocalDateTime generatedAt
    ) {
        this.summaryJson = summaryJson;
        this.sourceReviewCount = sourceReviewCount;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.modelVersion = modelVersion;
        this.generatedAt = generatedAt;
    }
}
