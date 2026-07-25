package com.ymall.backend.review.service;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewSummaryGenerator {

    Result generate(List<Input> reviews);

    record Input(
        int rating,
        String content,
        LocalDateTime updatedAt
    ) {
    }

    record Result(
        List<String> pros,
        List<String> cons,
        List<String> commonOpinions,
        String modelVersion
    ) {
    }
}
