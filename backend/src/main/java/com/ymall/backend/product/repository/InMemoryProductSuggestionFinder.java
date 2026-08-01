package com.ymall.backend.product.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.search.KoreanSearchNormalizer;
import com.ymall.backend.product.search.ProductSearchMatch;
import com.ymall.backend.product.search.ProductSearchScorer;

@Repository
@Profile("test")
@RequiredArgsConstructor
public class InMemoryProductSuggestionFinder implements ProductSuggestionFinder {

    private static final int MAXIMUM_CANDIDATES = 500;

    private final ProductRepository productRepository;

    @Override
    public List<ProductSearchMatch> findMatches(
        String normalizedKeyword,
        Set<Long> categoryIds,
        int limit
    ) {
        boolean choseongSearch = KoreanSearchNormalizer.isChoseongQuery(normalizedKeyword);
        return productRepository.findTop500ByStatusOrderByUpdatedAtDesc(ProductStatus.APPROVED)
            .stream()
            .filter(product -> categoryIds.isEmpty()
                || categoryIds.contains(product.getCategory().getId()))
            .limit(MAXIMUM_CANDIDATES)
            .map(product -> ProductSearchScorer.score(
                    product.getSearchNormalizedName(),
                    product.getSearchChosung(),
                    normalizedKeyword,
                    choseongSearch
                )
                .map(score -> new RankedMatch(
                    new ProductSearchMatch(
                        product.getId(),
                        product.getName(),
                        product.getThumbnailUrl(),
                        score.matchType(),
                        score.similarity(),
                        0
                    ),
                    score.rank()
                )))
            .flatMap(java.util.Optional::stream)
            .sorted(Comparator
                .comparingInt(RankedMatch::rank)
                .thenComparing(
                    RankedMatch::similarity,
                    Comparator.reverseOrder()
                )
                .thenComparing(RankedMatch::productId, Comparator.reverseOrder()))
            .limit(limit)
            .map(RankedMatch::match)
            .toList();
    }

    private record RankedMatch(ProductSearchMatch match, int rank) {

        private double similarity() {
            return match.similarity();
        }

        private Long productId() {
            return match.productId();
        }
    }
}
