package com.ymall.backend.product.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
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

    @NotNull(message = "상품 재고는 필수입니다.")
    @Min(value = 0, message = "상품 재고는 0 이상이어야 합니다.")
    Integer stock,

    String thumbnailUrl,

    @Valid
    List<ProductImageCreateRequest> images,

    @Valid
    List<ProductDetailImageCreateRequest> detailImages
) {
}
