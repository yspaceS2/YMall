package com.ymall.backend.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.cart.dto.CartItemResponse;
import com.ymall.backend.cart.entity.CartItem;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(source = "id", target = "cartItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "product.price", target = "price")
    @Mapping(source = "product.discountPercentage", target = "discountPercentage")
    @Mapping(source = "product.stock", target = "stock")
    @Mapping(source = "product.status", target = "productStatus")
    CartItemResponse toCartItemResponse(CartItem cartItem);
}
