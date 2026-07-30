package com.ymall.backend.admin.mapper;

import java.util.Comparator;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminOrderItemResponse;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.product.dto.ProductDetailImageResponse;
import com.ymall.backend.product.dto.ProductImageResponse;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductDetailImage;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.seller.entity.SellerProfile;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(source = "id", target = "productId")
    @Mapping(source = "sellerProfile.id", target = "sellerProfileId")
    @Mapping(source = "sellerProfile.storeName", target = "storeName")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "images", expression = "java(toSortedProductImageResponses(product.getImages()))")
    @Mapping(target = "detailImages", expression = "java(toSortedDetailImageResponses(product.getDetailImages()))")
    AdminProductResponse toProductResponse(Product product);

    default List<ProductImageResponse> toSortedProductImageResponses(List<ProductImage> images) {
        return images.stream()
            .sorted(Comparator.comparing(ProductImage::getSortOrder))
            .map(ProductImageResponse::from)
            .toList();
    }

    default List<ProductDetailImageResponse> toSortedDetailImageResponses(
        List<ProductDetailImage> images
    ) {
        return images.stream()
            .sorted(Comparator.comparing(ProductDetailImage::getSortOrder))
            .map(image -> new ProductDetailImageResponse(
                image.getId(),
                image.getOriginalUrl(),
                image.getImageUrl(),
                image.getSortOrder()
            ))
            .toList();
    }

    @Mapping(source = "id", target = "memberId")
    AdminMemberResponse toMemberResponse(Member member);

    @Mapping(source = "id", target = "sellerProfileId")
    @Mapping(source = "member.id", target = "memberId")
    @Mapping(source = "member.email", target = "email")
    @Mapping(source = "member.name", target = "memberName")
    AdminSellerResponse toSellerResponse(SellerProfile sellerProfile);

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.member.id", target = "memberId")
    @Mapping(source = "order.member.email", target = "memberEmail")
    @Mapping(source = "order.member.name", target = "memberName")
    @Mapping(source = "refundSupported", target = "refundSupported")
    AdminOrderResponse toOrderResponse(Order order, boolean refundSupported);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "effectiveFulfillmentStatus", target = "fulfillmentStatus")
    AdminOrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
