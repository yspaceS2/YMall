package com.ymall.backend.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductSnapshotResponse(
    Long categoryId,
    String categoryName,
    String name,
    String description,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    LocalDate discountStartDate,
    LocalDate discountEndDate,
    Integer stock,
    String thumbnailUrl,
    boolean freeShipping,
    BigDecimal shippingFee,
    Integer estimatedDeliveryDays,
    List<ProductImageSnapshot> images,
    List<ProductImageSnapshot> detailImages
) {
}
