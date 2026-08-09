package com.ymall.backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.entity.ReviewSummary;
import com.ymall.backend.review.repository.ReviewRepository;
import com.ymall.backend.review.repository.ReviewSummaryRepository;

class ReviewSummarySnapshotServiceTest {

    private ReviewRepository reviewRepository;
    private ReviewSummaryRepository summaryRepository;
    private ProductRepository productRepository;
    private ReviewSummaryCodec codec;
    private ReviewSummarySnapshotService service;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(ReviewRepository.class);
        summaryRepository = mock(ReviewSummaryRepository.class);
        productRepository = mock(ProductRepository.class);
        codec = mock(ReviewSummaryCodec.class);
        ReviewSummaryProperties properties = mock(ReviewSummaryProperties.class);
        given(properties.maximumReviews()).willReturn(10);
        given(properties.maximumReviewLength()).willReturn(5);
        given(properties.maximumTotalLength()).willReturn(7);
        service = new ReviewSummarySnapshotService(
            reviewRepository,
            summaryRepository,
            productRepository,
            properties,
            codec,
            Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void trimsAndBoundsInputsByReviewAndTotalLength() {
        Long productId = 10L;
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 5, 9, 0);
        given(reviewRepository.countByProductId(productId)).willReturn(3L);
        given(reviewRepository.findLatestUpdatedAtByProductId(productId))
            .willReturn(updatedAt);
        given(reviewRepository.findSummaryInputsByProductId(any(), any()))
            .willReturn(List.of(
                new ReviewSummaryGenerator.Input(5, " abcdef ", updatedAt),
                new ReviewSummaryGenerator.Input(4, "xy", updatedAt),
                new ReviewSummaryGenerator.Input(3, "z", updatedAt)
            ));
        given(summaryRepository.findByProductId(productId)).willReturn(Optional.empty());

        ReviewSummarySnapshotService.Snapshot snapshot = service.load(productId);

        assertThat(snapshot.reviewCount()).isEqualTo(3);
        assertThat(snapshot.inputs()).extracting(ReviewSummaryGenerator.Input::content)
            .containsExactly("abcde", "xy");
        assertThat(snapshot.matchesStoredSummary()).isFalse();
    }

    @Test
    void rejectsGeneratedResultWhenReviewsChangedDuringGeneration() {
        Long productId = 10L;
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 5, 9, 0);
        ReviewSummarySnapshotService.Snapshot expected =
            new ReviewSummarySnapshotService.Snapshot(
                2,
                updatedAt,
                List.of(),
                false
            );
        given(reviewRepository.countByProductId(productId)).willReturn(3L);
        given(reviewRepository.findLatestUpdatedAtByProductId(productId))
            .willReturn(updatedAt);

        assertThatThrownBy(() -> service.storeIfCurrent(
            productId,
            expected,
            mock(ReviewSummaryGenerator.Result.class)
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Reviews changed while the AI summary was being generated.");

        verify(summaryRepository, never()).save(any(ReviewSummary.class));
        verifyNoInteractions(codec, productRepository);
    }
}
