package com.ymall.backend.product.mapper;

import java.util.Comparator;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailImageCreateRequest;
import com.ymall.backend.product.dto.ProductDetailImageResponse;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductImageCreateRequest;
import com.ymall.backend.product.dto.ProductImageResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductDetailImage;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.product.entity.ProductStatus;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "id", target = "categoryId")
    CategoryResponse toCategoryResponse(Category category);

    @Mapping(source = "id", target = "imageId")
    ProductImageResponse toProductImageResponse(ProductImage productImage);

    @Mapping(source = "id", target = "detailImageId")
    ProductDetailImageResponse toProductDetailImageResponse(ProductDetailImage productDetailImage);

    @Mapping(source = "id", target = "productId")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    ProductListResponse toProductListResponse(Product product);

    @Mapping(source = "id", target = "productId")
    @Mapping(source = "category", target = "category")
    @Mapping(target = "images", expression = "java(toSortedProductImageResponses(product.getImages()))")
    @Mapping(target = "detailImages", expression = "java(toSortedProductDetailImageResponses(product.getDetailImages()))")
    ProductDetailResponse toProductDetailResponse(Product product);

    default List<ProductImageResponse> toSortedProductImageResponses(List<ProductImage> productImages) {
        return productImages.stream()
            .sorted(Comparator.comparing(ProductImage::getSortOrder))
            .map(this::toProductImageResponse)
            .toList();
    }

    default List<ProductDetailImageResponse> toSortedProductDetailImageResponses(
        List<ProductDetailImage> productDetailImages
    ) {
        return productDetailImages.stream()
            .sorted(Comparator.comparing(ProductDetailImage::getSortOrder))
            .map(this::toProductDetailImageResponse)
            .toList();
    }

    /**
     * 생성 요청은 외부 입력값만 포함하므로 카테고리와 상태는 서비스에서 검증 후 주입한다.
     * rating은 리뷰 도메인 집계 값이므로 상품 생성 시점에는 null로 둔다.
     */
    default Product toEntity(ProductCreateRequest request, Category category, ProductStatus status) {
        Product product = new Product(
            category,
            request.name(),
            request.description(),
            request.brand(),
            request.price(),
            request.discountPercentage(),
            null,
            request.stock(),
            request.thumbnailUrl(),
            status
        );

        if (request.images() != null) {
            request.images()
                .stream()
                .map(this::toEntity)
                .forEach(product::addImage);
        }

        if (request.detailImages() != null) {
            request.detailImages()
                .stream()
                .map(this::toEntity)
                .forEach(product::addDetailImage);
        }

        return product;
    }

    /**
     * 이미지 원본 URL과 현재 표시 URL을 분리해 둔다.
     * DummyJSON URL을 나중에 로컬 파일이나 S3 URL로 이전할 때 imageUrl만 교체하기 위함이다.
     */
    default ProductImage toEntity(ProductImageCreateRequest request) {
        return new ProductImage(
            request.originalUrl(),
            request.imageUrl(),
            request.sortOrder()
        );
    }

    default ProductDetailImage toEntity(ProductDetailImageCreateRequest request) {
        return new ProductDetailImage(
            request.originalUrl(),
            request.imageUrl(),
            request.sortOrder()
        );
    }

    /**
     * 수정 요청에서 images가 생략되면 빈 목록으로 간주한다.
     * Product.replaceImages 정책과 결합되어 기존 이미지가 모두 제거된다.
     */
    default List<ProductImage> toImageEntities(ProductUpdateRequest request) {
        if (request.images() == null) {
            return List.of();
        }

        return request.images()
            .stream()
            .map(this::toEntity)
            .toList();
    }

    default List<ProductDetailImage> toDetailImageEntities(ProductUpdateRequest request) {
        if (request.detailImages() == null) {
            return List.of();
        }

        return request.detailImages()
            .stream()
            .map(this::toEntity)
            .toList();
    }
}
