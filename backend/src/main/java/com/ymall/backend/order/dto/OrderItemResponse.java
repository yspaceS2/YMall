package com.ymall.backend.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long orderItemId,
    Long productId,
    String productName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalPrice
) {
}
