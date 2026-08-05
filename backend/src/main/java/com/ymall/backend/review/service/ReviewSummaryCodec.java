package com.ymall.backend.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ReviewSummaryCodec {

    private final ObjectMapper objectMapper;

    public String write(ReviewSummaryGenerator.Result result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Review summary could not be serialized.",
                exception
            );
        }
    }

    public ReviewSummaryGenerator.Result read(String summaryJson) {
        try {
            return objectMapper.readValue(
                summaryJson,
                ReviewSummaryGenerator.Result.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Stored review summary is invalid.", exception);
        }
    }
}
