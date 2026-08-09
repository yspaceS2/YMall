package com.ymall.backend.review.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.review.config.ReviewSummaryCacheNames;
import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.dto.ReviewSummaryResponse;
import com.ymall.backend.review.entity.ReviewSummary;
import com.ymall.backend.review.repository.ReviewRepository;
import com.ymall.backend.review.repository.ReviewSummaryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ProductRepository productRepository;
    private final ReviewSummaryGenerator generator;
    private final ReviewSummaryProperties properties;
    private final ReviewSummaryGenerationLockManager lockManager;
    private final ReviewSummarySnapshotService snapshotService;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;

    @Cacheable(cacheNames = ReviewSummaryCacheNames.BY_PRODUCT, key = "#productId")
    public ReviewSummaryResponse getSummary(Long productId) {
        if (!productRepository.existsByIdAndStatus(productId, ProductStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        long reviewCount = reviewRepository.countByProductId(productId);
        return reviewSummaryRepository.findByProductId(productId)
            .map(summary -> snapshotService.response(summary, reviewCount))
            .orElseGet(() -> ReviewSummaryResponse.unavailable(reviewCount));
    }

    /**
     * 현재 리뷰 Snapshot이 저장된 요약과 다를 때만 새 요약을 생성한다.
     *
     * <p>외부 AI 호출 중에는 DB 트랜잭션을 유지하지 않는다. 호출 전 Snapshot을 만들고 생성 후
     * 입력이 여전히 최신일 때만 저장하여, 추론 도중 변경된 리뷰를 오래된 결과로 덮어쓰지 않는다.
     * 최소 리뷰 수 미만이면 기존 요약을 제거하며, 상품별 잠금으로 중복 생성을 제한한다.</p>
     */
    public void refresh(Long productId) {
        if (!properties.enabled()) {
            return;
        }
        Optional<ReviewSummaryGenerationLockManager.GenerationLock> generationLock =
            lockManager.acquire(productId);
        if (generationLock.isEmpty()) {
            throw new IllegalStateException(
                "Review summary generation is already running for productId=" + productId
            );
        }

        try (ReviewSummaryGenerationLockManager.GenerationLock ignored =
            generationLock.get()) {
            ReviewSummarySnapshotService.Snapshot snapshot =
                transactionTemplate.execute(status -> snapshotService.load(productId));
            if (snapshot == null) {
                return;
            }
            if (snapshot.reviewCount() < properties.minimumReviews()) {
                transactionTemplate.executeWithoutResult(status ->
                    snapshotService.delete(productId)
                );
                evict(productId);
                return;
            }
            if (snapshot.matchesStoredSummary()) {
                return;
            }
            if (snapshot.inputs().isEmpty()) {
                throw new IllegalStateException(
                    "Review summary input is empty after applying limits."
                );
            }

            ReviewSummaryGenerator.Result result = generator.generate(snapshot.inputs());
            transactionTemplate.executeWithoutResult(status ->
                snapshotService.storeIfCurrent(productId, snapshot, result)
            );
            evict(productId);
        }
    }

    public void evict(Long productId) {
        Cache cache = cacheManager.getCache(ReviewSummaryCacheNames.BY_PRODUCT);
        if (cache != null) {
            try {
                cache.evict(productId);
            } catch (RuntimeException exception) {
                log.warn(
                    "Review summary cache eviction failed. productId={}, reason={}",
                    productId,
                    exception.getMessage()
                );
            }
        }
    }

}
