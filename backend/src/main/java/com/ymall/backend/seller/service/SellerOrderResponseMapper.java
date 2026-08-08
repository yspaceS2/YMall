package com.ymall.backend.seller.service;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.seller.dto.SellerDeliveryAddressResponse;
import com.ymall.backend.seller.dto.SellerOrderDetailResponse;
import com.ymall.backend.seller.dto.SellerOrderItemResponse;
import com.ymall.backend.seller.dto.SellerOrderResponse;

@Component
@RequiredArgsConstructor
public class SellerOrderResponseMapper {

    private final SellerDeliveryAddressPrivacyPolicy deliveryAddressPrivacyPolicy;

    public SellerOrderResponse toResponse(
        Order order,
        Long sellerProfileId,
        boolean refundSupported
    ) {
        List<SellerOrderItemResponse> items = toItemResponses(order, sellerProfileId);
        return new SellerOrderResponse(
            order.getId(),
            order.getStatus(),
            sellerAmount(items),
            order.getCreatedAt(),
            refundSupported,
            items
        );
    }

    public SellerOrderDetailResponse toDetail(
        Order order,
        Long sellerProfileId,
        boolean refundSupported
    ) {
        List<OrderItem> ownedItems = ownedItems(order, sellerProfileId);
        List<SellerOrderItemResponse> items = ownedItems.stream()
            .map(this::toItemResponse)
            .toList();
        return new SellerOrderDetailResponse(
            order.getId(),
            order.getStatus(),
            sellerAmount(items),
            order.getCreatedAt(),
            refundSupported,
            SellerDeliveryAddressResponse.from(
                order.getDeliveryAddress(),
                deliveryAddressPrivacyPolicy.shouldMask(ownedItems)
            ),
            items
        );
    }

    public List<OrderItem> ownedItems(Order order, Long sellerProfileId) {
        return order.getItems().stream()
            .filter(item -> item.getProduct().getSellerProfile() != null)
            .filter(item -> item.getProduct().getSellerProfile().getId().equals(sellerProfileId))
            .toList();
    }

    private List<SellerOrderItemResponse> toItemResponses(Order order, Long sellerProfileId) {
        return ownedItems(order, sellerProfileId).stream()
            .map(this::toItemResponse)
            .toList();
    }

    private BigDecimal sellerAmount(List<SellerOrderItemResponse> items) {
        return items.stream()
            .map(SellerOrderItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private SellerOrderItemResponse toItemResponse(OrderItem item) {
        return new SellerOrderItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProductName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getRefundedQuantity(),
            item.getLineTotal(),
            item.getProduct().getThumbnailUrl(),
            item.getEffectiveFulfillmentStatus(),
            item.getCarrier(),
            item.getTrackingNumber(),
            item.getShippedAt(),
            item.getDeliveredAt()
        );
    }
}
