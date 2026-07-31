package com.ymall.backend.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.order.dto.OrderItemResponse;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "refundSupported", target = "refundSupported")
    OrderResponse toOrderResponse(Order order, boolean refundSupported);

    default OrderResponse toOrderResponse(Order order) {
        return toOrderResponse(order, false);
    }

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "lineTotal", target = "totalPrice")
    @Mapping(target = "fulfillmentStatus", expression = "java(orderItem.getEffectiveFulfillmentStatus())")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
