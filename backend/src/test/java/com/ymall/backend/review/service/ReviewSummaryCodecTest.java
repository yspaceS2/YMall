package com.ymall.backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class ReviewSummaryCodecTest {

    private final ReviewSummaryCodec codec = new ReviewSummaryCodec(new ObjectMapper());

    @Test
    void preservesGeneratedResultThroughJsonRoundTrip() {
        ReviewSummaryGenerator.Result result = new ReviewSummaryGenerator.Result(
            List.of("장점"),
            List.of("단점"),
            List.of("공통 의견"),
            "test-model"
        );

        ReviewSummaryGenerator.Result decoded = codec.read(codec.write(result));

        assertThat(decoded).isEqualTo(result);
    }

    @Test
    void rejectsInvalidStoredJson() {
        assertThatThrownBy(() -> codec.read("not-json"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Stored review summary is invalid.");
    }
}
