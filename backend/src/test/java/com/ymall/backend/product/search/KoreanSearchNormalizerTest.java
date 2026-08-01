package com.ymall.backend.product.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoreanSearchNormalizerTest {

    @Test
    void removesWhitespaceAndNormalizesCase() {
        assertThat(KoreanSearchNormalizer.normalize("  Mac Book  PRO "))
            .isEqualTo("macbookpro");
    }

    @Test
    void convertsHangulSyllablesToChoseong() {
        assertThat(KoreanSearchNormalizer.toChoseong("노트북 파우치"))
            .isEqualTo("ㄴㅌㅂㅍㅇㅊ");
    }

    @Test
    void recognizesOnlyPureChoseongQueries() {
        assertThat(KoreanSearchNormalizer.isChoseongQuery("ㄴ ㅌ ㅂ")).isTrue();
        assertThat(KoreanSearchNormalizer.isChoseongQuery("노트북")).isFalse();
    }
}
