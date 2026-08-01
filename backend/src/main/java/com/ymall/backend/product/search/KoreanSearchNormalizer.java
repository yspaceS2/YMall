package com.ymall.backend.product.search;

import java.text.Normalizer;
import java.util.Locale;

public final class KoreanSearchNormalizer {

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final int HANGUL_SYLLABLE_BLOCK_SIZE = 588;
    private static final char[] CHOSEONG = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private KoreanSearchNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
            .toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        normalized.codePoints()
            .filter(codePoint -> !Character.isWhitespace(codePoint)
                && !Character.isSpaceChar(codePoint))
            .forEach(result::appendCodePoint);
        return result.toString();
    }

    public static String toChoseong(String value) {
        String normalized = normalize(value);
        StringBuilder result = new StringBuilder(normalized.length());

        normalized.codePoints().forEach(codePoint -> {
            if (codePoint >= HANGUL_BASE && codePoint <= HANGUL_LAST) {
                int index = (codePoint - HANGUL_BASE) / HANGUL_SYLLABLE_BLOCK_SIZE;
                result.append(CHOSEONG[index]);
            } else if (isChoseong(codePoint) || Character.isLetterOrDigit(codePoint)) {
                result.appendCodePoint(codePoint);
            }
        });
        return result.toString();
    }

    public static boolean isChoseongQuery(String value) {
        String normalized = normalize(value);
        return !normalized.isEmpty()
            && normalized.codePoints().allMatch(KoreanSearchNormalizer::isChoseong);
    }

    private static boolean isChoseong(int codePoint) {
        return codePoint >= 'ㄱ' && codePoint <= 'ㅎ';
    }
}
