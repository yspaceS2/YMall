package com.ymall.backend.wishlist.dto;

public record WishlistStatusResponse(
    Long productId,
    boolean wished
) {
}
