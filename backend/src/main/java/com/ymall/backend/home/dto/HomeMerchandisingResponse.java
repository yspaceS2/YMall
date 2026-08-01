package com.ymall.backend.home.dto;

import java.util.List;

public record HomeMerchandisingResponse(
    List<HomeMerchandisingGroupResponse> categoryBest,
    List<HomeMerchandisingGroupResponse> grocery,
    List<HomeMerchandisingGroupResponse> fashion,
    List<HomeMerchandisingProductResponse> newArrivals
) {
}
