package com.ymall.backend.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ymall.backend.order.dto.OrderItemResponse;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    OrderResponse toOrderResponse(Order order);

    @Mapping(source = "id", target = "orderItemId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "lineTotal", target = "totalPrice")
    OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
