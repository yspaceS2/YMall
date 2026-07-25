package com.ymall.backend.review.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ymall.ai.review-summary")
public record ReviewSummaryProperties(
    boolean enabled,
    URI baseUrl,
    String model,
    int minimumReviews,
    int maximumReviews,
    int maximumReviewLength,
    int maximumTotalLength,
    int maximumTokens,
    Duration connectTimeout,
    Duration readTimeout,
    Duration lockTtl
) {

    public ReviewSummaryProperties {
        if (minimumReviews < 1 || maximumReviews < minimumReviews) {
            throw new IllegalArgumentException("Review summary count settings are invalid.");
        }
        if (maximumReviewLength < 1 || maximumTotalLength < maximumReviewLength) {
            throw new IllegalArgumentException("Review summary content settings are invalid.");
        }
        if (maximumTokens < 1) {
            throw new IllegalArgumentException("Review summary token setting is invalid.");
        }
        if (baseUrl == null || model == null || model.isBlank()) {
            throw new IllegalArgumentException("Review summary model settings are required.");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
            || readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()
            || lockTtl == null || lockTtl.isNegative() || lockTtl.isZero()) {
            throw new IllegalArgumentException("Review summary timeout settings are invalid.");
        }
    }
}
