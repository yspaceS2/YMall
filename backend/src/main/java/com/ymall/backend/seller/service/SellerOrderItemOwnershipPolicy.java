package com.ymall.backend.seller.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;

@Component
public class SellerOrderItemOwnershipPolicy {

    public List<OrderItem> ownedItems(Order order, Long sellerProfileId) {
        return order.getItems().stream()
            .filter(item -> item.getProduct().getSellerProfile() != null)
            .filter(item -> item.getProduct().getSellerProfile().getId().equals(sellerProfileId))
            .toList();
    }
}
