package com.ymall.backend.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductDetailImage;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.product.entity.ProductStatus;

public record ProductDetailResponse(
    Long productId,
    CategoryResponse category,
    String name,
    String description,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    LocalDate discountStartDate,
    LocalDate discountEndDate,
    BigDecimal rating,
    Integer stock,
    String thumbnailUrl,
    boolean freeShipping,
    BigDecimal shippingFee,
    Integer estimatedDeliveryDays,
    ProductStatus status,
    List<ProductImageResponse> images,
    List<ProductDetailImageResponse> detailImages
) {
    public ProductDetailResponse(
        Long productId,
        CategoryResponse category,
        String name,
        String description,
        String brand,
        BigDecimal price,
        BigDecimal discountPercentage,
        BigDecimal rating,
        Integer stock,
        String thumbnailUrl,
        ProductStatus status,
        List<ProductImageResponse> images,
        List<ProductDetailImageResponse> detailImages
    ) {
        this(
            productId,
            category,
            name,
            description,
            brand,
            price,
            discountPercentage,
            null,
            null,
            rating,
            stock,
            thumbnailUrl,
            true,
            BigDecimal.ZERO,
            3,
            status,
            images,
            detailImages
        );
    }

    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
            product.getId(),
            CategoryResponse.from(product.getCategory()),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            product.getPrice(),
            product.getEffectiveDiscountPercentage(),
            product.getDiscountStartDate(),
            product.getDiscountEndDate(),
            product.getRating(),
            product.getStock(),
            product.getThumbnailUrl(),
            product.isFreeShipping(),
            product.getEffectiveShippingFee(),
            product.getEstimatedDeliveryDays(),
            product.getStatus(),
            product.getImages()
                .stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder))
                .map(ProductImageResponse::from)
                .toList(),
            product.getDetailImages()
                .stream()
                .sorted(Comparator.comparing(ProductDetailImage::getSortOrder))
                .map(image -> new ProductDetailImageResponse(
                    image.getId(),
                    image.getOriginalUrl(),
                    image.getImageUrl(),
                    image.getSortOrder()
                ))
                .toList()
        );
    }
}
