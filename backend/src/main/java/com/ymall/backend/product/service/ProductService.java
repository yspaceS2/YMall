package com.ymall.backend.product.service;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.dto.ProductSuggestionResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.repository.ProductSuggestionFinder;
import com.ymall.backend.product.search.KoreanSearchNormalizer;
import com.ymall.backend.product.search.ProductSearchMatch;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SUGGESTION_SIZE = 8;
    private static final int MAX_FUZZY_SEARCH_RESULTS = 100;

    private final ProductRepository productRepository;
    private final ProductSuggestionFinder productSuggestionFinder;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductCacheInvalidator productCacheInvalidator;

    /**
     * 사용자에게 노출되는 상품 목록은 승인된 상품만 대상으로 한다.
     * 판매자 검수나 삭제 상태의 상품은 별도 관리 API에서 다루도록 분리한다.
     */
    public PageResponse<ProductListResponse> getProducts(int page, int size) {
        Pageable pageable = createPageable(page, size);

        return PageResponse.from(
            productRepository.findByStatus(ProductStatus.APPROVED, pageable)
                .map(productMapper::toProductListResponse)
        );
    }

    /**
     * 상품 상세 조회도 공개 목록과 동일하게 APPROVED 상태만 허용한다.
     * DELETED, DRAFT, PENDING 상품은 외부 사용자가 존재 여부를 알 수 없도록 404로 처리한다.
     */
    @Cacheable(cacheNames = ProductCacheNames.DETAILS, key = "#productId", sync = true)
    public ProductDetailResponse getProduct(Long productId) {
        return productRepository.findWithCategoryAndImagesById(productId)
            .filter(product -> product.getStatus() == ProductStatus.APPROVED)
            .map(productMapper::toProductDetailResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public PageResponse<ProductListResponse> searchProducts(
        String keyword,
        Long categoryId,
        int page,
        int size
    ) {
        Pageable pageable = createPageable(page, size);
        String normalizedKeyword = KoreanSearchNormalizer.normalize(keyword);
        if (normalizedKeyword.isEmpty()) {
            return categoryId == null
                ? getProducts(page, size)
                : getProductsByCategory(categoryId, page, size);
        }
        Set<Long> categoryIds = categoryId == null
            ? Set.of()
            : getSearchCategoryIds(categoryId);
        String choseongKeyword = KoreanSearchNormalizer.isChoseongQuery(normalizedKeyword)
            ? normalizedKeyword
            : "";

        Page<Product> exactMatches = productRepository.searchPublicProducts(
            normalizedKeyword,
            choseongKeyword,
            ProductStatus.APPROVED,
            !categoryIds.isEmpty(),
            categoryIds.isEmpty() ? Set.of(-1L) : categoryIds,
            pageable
        );
        if (exactMatches.getTotalElements() > 0) {
            return PageResponse.from(exactMatches.map(productMapper::toProductListResponse));
        }

        return fuzzySearch(normalizedKeyword, categoryIds, page, size);
    }

    public List<ProductSuggestionResponse> getProductSuggestions(
        String keyword,
        Long categoryId,
        int size
    ) {
        String normalizedKeyword = KoreanSearchNormalizer.normalize(keyword);
        if (normalizedKeyword.length() < 2) {
            return List.of();
        }
        Set<Long> categoryIds = categoryId == null
            ? Set.of()
            : getSearchCategoryIds(categoryId);
        int suggestionSize = Math.min(Math.max(size, 1), MAX_SUGGESTION_SIZE);
        return productSuggestionFinder.findMatches(
                normalizedKeyword,
                categoryIds,
                suggestionSize
            )
            .stream()
            .map(ProductSuggestionResponse::from)
            .toList();
    }

    private PageResponse<ProductListResponse> fuzzySearch(
        String normalizedKeyword,
        Set<Long> categoryIds,
        int page,
        int size
    ) {
        List<ProductSearchMatch> matches = productSuggestionFinder.findMatches(
            normalizedKeyword,
            categoryIds,
            MAX_FUZZY_SEARCH_RESULTS
        );
        int pageNumber = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int fromIndex = Math.min((pageNumber - 1) * pageSize, matches.size());
        int toIndex = Math.min(fromIndex + pageSize, matches.size());
        List<ProductSearchMatch> pageMatches = matches.subList(fromIndex, toIndex);

        Map<Long, Product> productsById = pageMatches.isEmpty()
            ? Map.of()
            : productRepository.findByIdIn(
                    pageMatches.stream().map(ProductSearchMatch::productId).toList()
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    Product::getId,
                    product -> product,
                    (left, right) -> left,
                    LinkedHashMap::new
                ));
        List<ProductListResponse> content = pageMatches.stream()
            .map(match -> productsById.get(match.productId()))
            .filter(java.util.Objects::nonNull)
            .map(productMapper::toProductListResponse)
            .toList();
        int totalPages = matches.isEmpty()
            ? 0
            : (int) Math.ceil((double) matches.size() / pageSize);

        return new PageResponse<>(
            content,
            pageNumber,
            pageSize,
            matches.size(),
            totalPages,
            pageNumber < totalPages,
            pageNumber > 1 && fromIndex > 0
        );
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findByActiveTrue(
                Sort.by(
                    Sort.Order.asc("depth"),
                    Sort.Order.asc("displayOrder"),
                    Sort.Order.asc("name")
                )
            )
            .stream()
            .filter(this::isCategoryPathActive)
            .map(productMapper::toCategoryResponse)
            .toList();
    }

    public PageResponse<ProductListResponse> getProductsByCategory(Long categoryId, int page, int size) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!isCategoryPathActive(category)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        Set<Long> categoryIds = getActiveCategoryTreeIds(categoryId);

        return PageResponse.from(
            productRepository.findByCategoryIdInAndStatus(
                    categoryIds,
                    ProductStatus.APPROVED,
                    createPageable(page, size)
                )
                .map(productMapper::toProductListResponse)
        );
    }

    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        Category category = getCategory(request.categoryId());
        Product product = productMapper.toEntity(request, category, ProductStatus.APPROVED);
        Product savedProduct = productRepository.save(product);

        return productMapper.toProductDetailResponse(savedProduct);
    }

    /**
     * 상품 수정은 요청에 포함된 이미지 목록을 기준으로 전체 교체한다.
     * 부분 수정 방식은 이미지 정렬과 삭제 처리가 복잡해지므로 MVP에서는 전체 교체 정책을 사용한다.
     */
    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = getProductEntity(productId);
        Category category = getCategory(request.categoryId());

        product.update(
            category,
            request.name(),
            request.description(),
            request.brand(),
            request.price(),
            request.discountPercentage(),
            request.discountStartDate(),
            request.discountEndDate(),
            request.freeShipping(),
            request.shippingFee() == null ? java.math.BigDecimal.ZERO : request.shippingFee(),
            request.estimatedDeliveryDays(),
            request.stock(),
            request.thumbnailUrl()
        );
        product.replaceImages(productMapper.toImageEntities(request));
        product.replaceDetailImages(productMapper.toDetailImageEntities(request));

        productCacheInvalidator.evictDetail(productId);

        return productMapper.toProductDetailResponse(product);
    }

    /**
     * 상품 삭제는 주문, 장바구니, 리뷰 등 참조 이력 보존을 위해 물리 삭제하지 않는다.
     * 상태만 DELETED로 전환하고 공개 조회 쿼리에서 제외한다.
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProductEntity(productId);

        product.delete();
        productCacheInvalidator.evictDetail(productId);
    }

    /**
     * 프론트엔드는 1부터 시작하는 page 값을 사용하고,
     * Spring Data PageRequest는 0부터 시작하므로 여기에서 변환한다.
     * 과도한 요청을 막기 위해 size는 MAX_PAGE_SIZE 이하로 제한한다.
     */
    private Pageable createPageable(int page, int size) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Category getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        validateSelectableCategory(category);
        return category;
    }

    private Set<Long> getActiveCategoryTreeIds(Long rootCategoryId) {
        List<Category> activeCategories = categoryRepository.findByActiveTrue(Sort.unsorted());
        Set<Long> categoryIds = new HashSet<>();
        categoryIds.add(rootCategoryId);

        boolean categoryAdded;
        do {
            categoryAdded = false;
            for (Category category : activeCategories) {
                Category parent = category.getParent();
                if (parent != null
                    && categoryIds.contains(parent.getId())
                    && categoryIds.add(category.getId())) {
                    categoryAdded = true;
                }
            }
        } while (categoryAdded);

        return categoryIds;
    }

    private Set<Long> getSearchCategoryIds(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!isCategoryPathActive(category)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return getActiveCategoryTreeIds(categoryId);
    }

    private void validateSelectableCategory(Category category) {
        if (!isCategoryPathActive(category)
            || categoryRepository.existsByParentId(category.getId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_SELECTABLE);
        }
    }

    private boolean isCategoryPathActive(Category category) {
        Category cursor = category;
        while (cursor != null) {
            if (!cursor.isActive()) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    /**
     * 수정과 삭제 대상 상품은 공개 상태뿐 아니라 비공개 상태도 관리 대상이 될 수 있다.
     * 다만 이미 삭제 처리된 상품은 재수정/재삭제를 막기 위해 조회 대상에서 제외한다.
     */
    private Product getProductEntity(Long productId) {
        return productRepository.findWithCategoryAndImagesById(productId)
            .filter(product -> product.getStatus() != ProductStatus.DELETED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
