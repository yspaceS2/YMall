package com.ymall.backend.product.search;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ProductSearchScorer {

    private static final double MINIMUM_TRIGRAM_SIMILARITY = 0.3;
    private static final double MINIMUM_EDIT_SIMILARITY = 0.6;

    private ProductSearchScorer() {
    }

    public static Optional<Score> score(
        String normalizedProductName,
        String productChoseong,
        String normalizedKeyword,
        boolean choseongSearch
    ) {
        if (normalizedKeyword.isEmpty()) {
            return Optional.empty();
        }
        if (normalizedProductName.equals(normalizedKeyword)) {
            return Optional.of(new Score(0, 1.0, ProductSearchMatchType.EXACT));
        }
        if (normalizedProductName.startsWith(normalizedKeyword)) {
            return Optional.of(new Score(1, 1.0, ProductSearchMatchType.PREFIX));
        }
        if (normalizedProductName.contains(normalizedKeyword)) {
            return Optional.of(new Score(2, 1.0, ProductSearchMatchType.CONTAINS));
        }
        if (choseongSearch && productChoseong.contains(normalizedKeyword)) {
            return Optional.of(new Score(3, 1.0, ProductSearchMatchType.CHOSEONG));
        }

        double trigramSimilarity = trigramSimilarity(normalizedProductName, normalizedKeyword);
        double editSimilarity = editSimilarity(normalizedProductName, normalizedKeyword);
        if (trigramSimilarity < MINIMUM_TRIGRAM_SIMILARITY
            && editSimilarity < MINIMUM_EDIT_SIMILARITY) {
            return Optional.empty();
        }
        return Optional.of(new Score(
            4,
            Math.max(trigramSimilarity, editSimilarity),
            ProductSearchMatchType.FUZZY
        ));
    }

    private static double trigramSimilarity(String left, String right) {
        Set<String> leftTrigrams = trigrams(left);
        Set<String> rightTrigrams = trigrams(right);
        if (leftTrigrams.isEmpty() || rightTrigrams.isEmpty()) {
            return 0;
        }

        long intersection = leftTrigrams.stream().filter(rightTrigrams::contains).count();
        return (2.0 * intersection) / (leftTrigrams.size() + rightTrigrams.size());
    }

    private static Set<String> trigrams(String value) {
        String padded = "  " + value + " ";
        Set<String> result = new HashSet<>();
        for (int index = 0; index <= padded.length() - 3; index++) {
            result.add(padded.substring(index, index + 3));
        }
        return result;
    }

    private static double editSimilarity(String left, String right) {
        int maximumLength = Math.max(left.length(), right.length());
        if (maximumLength == 0) {
            return 1.0;
        }
        return 1.0 - ((double) levenshteinDistance(left, right) / maximumLength);
    }

    private static int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int substitutionCost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                    Math.min(current[column - 1] + 1, previous[column] + 1),
                    previous[column - 1] + substitutionCost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    public record Score(int rank, double similarity, ProductSearchMatchType matchType) {
    }
}
