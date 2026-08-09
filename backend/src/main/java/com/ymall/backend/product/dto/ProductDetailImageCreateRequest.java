package com.ymall.backend.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductDetailImageCreateRequest(
    String originalUrl,

    @NotBlank(message = "상품 상세 이미지 URL은 필수입니다.")
    String imageUrl,

    @NotNull(message = "상품 상세 이미지 순서는 필수입니다.")
    @Min(value = 0, message = "상품 상세 이미지 순서는 0 이상이어야 합니다.")
    Integer sortOrder
) {
}
