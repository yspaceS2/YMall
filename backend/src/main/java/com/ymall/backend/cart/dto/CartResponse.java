package com.ymall.backend.cart.dto;

import java.util.List;

public record CartResponse(
    List<CartItemResponse> items
) {
}
