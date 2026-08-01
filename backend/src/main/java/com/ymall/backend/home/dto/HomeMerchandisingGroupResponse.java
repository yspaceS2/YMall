package com.ymall.backend.home.dto;

import java.util.List;

public record HomeMerchandisingGroupResponse(
    Long categoryId,
    String categoryName,
    String categorySlug,
    List<HomeMerchandisingProductResponse> products
) {
}
