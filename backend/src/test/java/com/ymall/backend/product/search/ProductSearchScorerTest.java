package com.ymall.backend.product.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductSearchScorerTest {

    @Test
    void ranksChoseongMatchBeforeFuzzyMatch() {
        ProductSearchScorer.Score score = ProductSearchScorer.score(
                "노트북파우치",
                "ㄴㅌㅂㅍㅇㅊ",
                "ㄴㅌㅂ",
                true
            )
            .orElseThrow();

        assertThat(score.matchType()).isEqualTo(ProductSearchMatchType.CHOSEONG);
        assertThat(score.rank()).isEqualTo(3);
    }

    @Test
    void acceptsMinorTypoAsFuzzyMatch() {
        ProductSearchScorer.Score score = ProductSearchScorer.score(
                "노트북파우치",
                "ㄴㅌㅂㅍㅇㅊ",
                "노트북파취",
                false
            )
            .orElseThrow();

        assertThat(score.matchType()).isEqualTo(ProductSearchMatchType.FUZZY);
    }

    @Test
    void rejectsUnrelatedKeyword() {
        assertThat(ProductSearchScorer.score(
            "노트북파우치",
            "ㄴㅌㅂㅍㅇㅊ",
            "유기농사과",
            false
        )).isEmpty();
    }
}
