package com.ymall.backend.review.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.dto.ReviewSummaryResponse;
import com.ymall.backend.review.entity.ReviewSummary;
import com.ymall.backend.review.repository.ReviewRepository;
import com.ymall.backend.review.repository.ReviewSummaryRepository;

@Service
@RequiredArgsConstructor
public class ReviewSummarySnapshotService {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ProductRepository productRepository;
    private final ReviewSummaryProperties properties;
    private final ReviewSummaryCodec codec;
    private final Clock clock;

    public Snapshot load(Long productId) {
        long reviewCount = reviewRepository.countByProductId(productId);
        LocalDateTime sourceUpdatedAt =
            reviewRepository.findLatestUpdatedAtByProductId(productId);
        List<ReviewSummaryGenerator.Input> inputs =
            reviewRepository.findSummaryInputsByProductId(
                productId,
                PageRequest.of(0, properties.maximumReviews())
            );
        boolean matchesStoredSummary = reviewSummaryRepository.findByProductId(productId)
            .filter(summary -> summary.getSourceReviewCount() == reviewCount)
            .filter(summary -> sameTime(summary.getSourceUpdatedAt(), sourceUpdatedAt))
            .isPresent();
        return new Snapshot(
            reviewCount,
            sourceUpdatedAt,
            boundedInputs(inputs),
            matchesStoredSummary
        );
    }

    public void delete(Long productId) {
        reviewSummaryRepository.deleteByProductId(productId);
    }

    public void storeIfCurrent(
        Long productId,
        Snapshot expected,
        ReviewSummaryGenerator.Result result
    ) {
        // 생성된 결과가 더 최신 리뷰 Snapshot을 덮어쓰지 않도록 저장 직전에 다시 확인한다.
        long currentCount = reviewRepository.countByProductId(productId);
        LocalDateTime currentUpdatedAt =
            reviewRepository.findLatestUpdatedAtByProductId(productId);
        if (currentCount != expected.reviewCount()
            || !sameTime(currentUpdatedAt, expected.sourceUpdatedAt())) {
            throw new IllegalStateException(
                "Reviews changed while the AI summary was being generated."
            );
        }

        LocalDateTime generatedAt = LocalDateTime.now(clock);
        String summaryJson = codec.write(result);
        ReviewSummary summary = reviewSummaryRepository.findByProductId(productId)
            .orElseGet(() -> new ReviewSummary(
                findProduct(productId),
                summaryJson,
                currentCount,
                currentUpdatedAt,
                result.modelVersion(),
                generatedAt
            ));
        summary.update(
            summaryJson,
            currentCount,
            currentUpdatedAt,
            result.modelVersion(),
            generatedAt
        );
        reviewSummaryRepository.save(summary);
    }

    public ReviewSummaryResponse response(ReviewSummary summary, long currentReviewCount) {
        ReviewSummaryGenerator.Result result = codec.read(summary.getSummaryJson());
        return new ReviewSummaryResponse(
            true,
            currentReviewCount,
            result.pros(),
            result.cons(),
            result.commonOpinions(),
            summary.getModelVersion(),
            summary.getGeneratedAt().toInstant(ZoneOffset.UTC)
        );
    }

    private List<ReviewSummaryGenerator.Input> boundedInputs(
        List<ReviewSummaryGenerator.Input> inputs
    ) {
        List<ReviewSummaryGenerator.Input> bounded = new ArrayList<>();
        int totalLength = 0;
        for (ReviewSummaryGenerator.Input input : inputs) {
            String content = input.content().trim();
            if (content.length() > properties.maximumReviewLength()) {
                content = content.substring(0, properties.maximumReviewLength());
            }
            if (totalLength + content.length() > properties.maximumTotalLength()) {
                break;
            }
            bounded.add(new ReviewSummaryGenerator.Input(
                input.rating(),
                content,
                input.updatedAt()
            ));
            totalLength += content.length();
        }
        return List.copyOf(bounded);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private boolean sameTime(LocalDateTime left, LocalDateTime right) {
        return left == null ? right == null : left.equals(right);
    }

    public record Snapshot(
        long reviewCount,
        LocalDateTime sourceUpdatedAt,
        List<ReviewSummaryGenerator.Input> inputs,
        boolean matchesStoredSummary
    ) {
    }
}
