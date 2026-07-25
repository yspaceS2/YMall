package com.ymall.backend.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewSummaryResponse(
    boolean available,
    long reviewCount,
    List<String> pros,
    List<String> cons,
    List<String> commonOpinions,
    String modelVersion,
    LocalDateTime generatedAt
) {

    public static ReviewSummaryResponse unavailable(long reviewCount) {
        return new ReviewSummaryResponse(
            false,
            reviewCount,
            List.of(),
            List.of(),
            List.of(),
            null,
            null
        );
    }
}
