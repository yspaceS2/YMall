package com.ymall.backend.admin.mapper;

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
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.seller.entity.SellerProfile;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(source = "id", target = "productId")
    @Mapping(source = "sellerProfile.id", target = "sellerProfileId")
    @Mapping(source = "sellerProfile.storeName", target = "storeName")
    @Mapping(source = "category.name", target = "categoryName")
    AdminProductResponse toProductResponse(Product product);

    @Mapping(source = "id", target = "memberId")
    AdminMemberResponse toMemberResponse(Member member);

    @Mapping(source = "id", target = "sellerProfileId")
    @Mapping(source = "member.id", target = "memberId")
    @Mapping(source = "member.email", target = "email")
    @Mapping(source = "member.name", target = "memberName")
    AdminSellerResponse toSellerResponse(SellerProfile sellerProfile);

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "member.id", target = "memberId")
    @Mapping(source = "member.email", target = "memberEmail")
    @Mapping(source = "member.name", target = "memberName")
    AdminOrderResponse toOrderResponse(Order order);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "effectiveFulfillmentStatus", target = "fulfillmentStatus")
    AdminOrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
