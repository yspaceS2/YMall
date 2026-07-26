package com.ymall.backend.review.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Product;
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

    private static final String LOCK_KEY_PREFIX = "review-summary:generation:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
        new DefaultRedisScript<>(
            """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                end
                return 0
                """,
            Long.class
        );

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryRepository reviewSummaryRepository;
    private final ProductRepository productRepository;
    private final ReviewSummaryGenerator generator;
    private final ReviewSummaryProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final Set<Long> localLocks = ConcurrentHashMap.newKeySet();

    @Cacheable(cacheNames = ReviewSummaryCacheNames.BY_PRODUCT, key = "#productId")
    public ReviewSummaryResponse getSummary(Long productId) {
        if (!productRepository.existsByIdAndStatus(productId, ProductStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        long reviewCount = reviewRepository.countByProductId(productId);
        return reviewSummaryRepository.findByProductId(productId)
            .map(summary -> response(summary, reviewCount))
            .orElseGet(() -> ReviewSummaryResponse.unavailable(reviewCount));
    }

    public void refresh(Long productId) {
        if (!properties.enabled()) {
            return;
        }
        Optional<GenerationLock> generationLock = acquireLock(productId);
        if (generationLock.isEmpty()) {
            throw new IllegalStateException(
                "Review summary generation is already running for productId=" + productId
            );
        }

        try (GenerationLock ignored = generationLock.get()) {
            Snapshot snapshot = transactionTemplate.execute(status -> loadSnapshot(productId));
            if (snapshot == null) {
                return;
            }
            if (snapshot.reviewCount() < properties.minimumReviews()) {
                transactionTemplate.executeWithoutResult(status ->
                    reviewSummaryRepository.deleteByProductId(productId)
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
                storeIfCurrent(productId, snapshot, result)
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

    private Snapshot loadSnapshot(Long productId) {
        long reviewCount = reviewRepository.countByProductId(productId);
        LocalDateTime sourceUpdatedAt = reviewRepository.findLatestUpdatedAtByProductId(productId);
        List<ReviewSummaryGenerator.Input> inputs = reviewRepository.findSummaryInputsByProductId(
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

    private void storeIfCurrent(
        Long productId,
        Snapshot expected,
        ReviewSummaryGenerator.Result result
    ) {
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
        String summaryJson = writeResult(result);
        ReviewSummary summary = reviewSummaryRepository.findByProductId(productId)
            .orElseGet(() -> {
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
                return new ReviewSummary(
                    product,
                    summaryJson,
                    currentCount,
                    currentUpdatedAt,
                    result.modelVersion(),
                    generatedAt
                );
            });
        summary.update(
            summaryJson,
            currentCount,
            currentUpdatedAt,
            result.modelVersion(),
            generatedAt
        );
        reviewSummaryRepository.save(summary);
    }

    private ReviewSummaryResponse response(ReviewSummary summary, long currentReviewCount) {
        ReviewSummaryGenerator.Result result = readResult(summary.getSummaryJson());
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

    private String writeResult(ReviewSummaryGenerator.Result result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Review summary could not be serialized.", exception);
        }
    }

    private ReviewSummaryGenerator.Result readResult(String summaryJson) {
        try {
            return objectMapper.readValue(summaryJson, ReviewSummaryGenerator.Result.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored review summary is invalid.", exception);
        }
    }

    private Optional<GenerationLock> acquireLock(Long productId) {
        String key = LOCK_KEY_PREFIX + productId;
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key, token, properties.lockTtl());
            return Boolean.TRUE.equals(acquired)
                ? Optional.of(new GenerationLock(productId, key, token, true))
                : Optional.empty();
        } catch (RuntimeException exception) {
            log.warn(
                "Redis review summary lock is unavailable. Falling back to a local lock. productId={}",
                productId
            );
            return localLocks.add(productId)
                ? Optional.of(new GenerationLock(productId, key, token, false))
                : Optional.empty();
        }
    }

    private boolean sameTime(LocalDateTime left, LocalDateTime right) {
        return left == null ? right == null : left.equals(right);
    }

    private record Snapshot(
        long reviewCount,
        LocalDateTime sourceUpdatedAt,
        List<ReviewSummaryGenerator.Input> inputs,
        boolean matchesStoredSummary
    ) {
    }

    private final class GenerationLock implements AutoCloseable {

        private final Long productId;
        private final String key;
        private final String token;
        private final boolean redis;

        private GenerationLock(Long productId, String key, String token, boolean redis) {
            this.productId = productId;
            this.key = key;
            this.token = token;
            this.redis = redis;
        }

        @Override
        public void close() {
            if (!redis) {
                localLocks.remove(productId);
                return;
            }
            try {
                redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), token);
            } catch (RuntimeException exception) {
                log.warn("Redis review summary lock release failed. productId={}", productId);
            }
        }
    }
}
