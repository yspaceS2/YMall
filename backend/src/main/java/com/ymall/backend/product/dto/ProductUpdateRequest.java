package com.ymall.backend.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record ProductUpdateRequest(
    @NotNull(message = "카테고리는 필수입니다.")
    Long categoryId,

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 255, message = "상품명은 255자 이하여야 합니다.")
    String name,

    String description,

    @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
    String brand,

    @NotNull(message = "상품 가격은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = false, message = "상품 가격은 0보다 커야 합니다.")
    BigDecimal price,

    @DecimalMin(value = "0.0", message = "할인율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.0", message = "할인율은 100 이하여야 합니다.")
    BigDecimal discountPercentage,

    LocalDate discountStartDate,

    LocalDate discountEndDate,

    @NotNull(message = "상품 재고는 필수입니다.")
    @Min(value = 0, message = "상품 재고는 0 이상이어야 합니다.")
    Integer stock,

    String thumbnailUrl,

    @NotNull
    Boolean freeShipping,

    @DecimalMin(value = "0.0")
    BigDecimal shippingFee,

    @NotNull
    @Min(1)
    Integer estimatedDeliveryDays,

    @Valid
    List<ProductImageCreateRequest> images,

    @Valid
    List<ProductDetailImageCreateRequest> detailImages
) {
    public ProductUpdateRequest(
        Long categoryId,
        String name,
        String description,
        String brand,
        BigDecimal price,
        BigDecimal discountPercentage,
        Integer stock,
        String thumbnailUrl,
        List<ProductImageCreateRequest> images,
        List<ProductDetailImageCreateRequest> detailImages
    ) {
        this(
            categoryId,
            name,
            description,
            brand,
            price,
            discountPercentage,
            discountPercentage != null && discountPercentage.signum() > 0
                ? LocalDate.of(2000, 1, 1)
                : null,
            discountPercentage != null && discountPercentage.signum() > 0
                ? LocalDate.of(2100, 1, 1)
                : null,
            stock,
            thumbnailUrl,
            true,
            BigDecimal.ZERO,
            3,
            images,
            detailImages
        );
    }

    @AssertTrue(message = "할인 시작일과 종료일을 올바르게 입력해 주세요.")
    public boolean isDiscountPeriodValid() {
        if (discountPercentage == null || discountPercentage.signum() <= 0) {
            return discountStartDate == null && discountEndDate == null;
        }
        return discountStartDate != null
            && discountEndDate != null
            && !discountStartDate.isAfter(discountEndDate);
    }

    @AssertTrue(message = "유료 배송 상품은 배송비를 입력해 주세요.")
    public boolean isShippingPolicyValid() {
        return Boolean.TRUE.equals(freeShipping)
            ? shippingFee == null || shippingFee.signum() == 0
            : shippingFee != null && shippingFee.signum() > 0;
    }
}
