package com.ymall.backend.seller.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.event.NotificationEventPublisher;
import com.ymall.backend.seller.dto.SellerOrderItemResponse;
import com.ymall.backend.seller.dto.SellerOrderResponse;
import com.ymall.backend.seller.dto.SellerOrderStatusUpdateRequest;
import com.ymall.backend.seller.entity.SellerProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final EnumSet<OrderStatus> SELLER_VISIBLE_STATUSES = EnumSet.of(
        OrderStatus.PAID,
        OrderStatus.PREPARING,
        OrderStatus.SHIPPED,
        OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final SellerProfileService sellerProfileService;
    private final NotificationEventPublisher notificationEventPublisher;

    public PageResponse<SellerOrderResponse> getOrders(Long memberId, int page, int size) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
        Page<SellerOrderResponse> orders = orderRepository.findSellerOrders(
            profile.getId(),
            SELLER_VISIBLE_STATUSES,
            pageable
        ).map(order -> toResponse(order, profile.getId()));
        return PageResponse.from(orders);
    }

    @Transactional
    public SellerOrderResponse updateStatus(
        Long memberId,
        Long orderId,
        SellerOrderStatusUpdateRequest request
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Order order = orderRepository.findSellerOrderByIdForUpdate(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        if (!SELLER_VISIBLE_STATUSES.contains(order.getStatus())
            || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }

        List<OrderItem> sellerItems = ownedItems(order, profile.getId());
        boolean statusChanged = sellerItems.stream().anyMatch(item ->
            item.getEffectiveFulfillmentStatus() != request.fulfillmentStatus()
        );
        try {
            sellerItems.forEach(item ->
                item.updateFulfillmentStatus(request.fulfillmentStatus())
            );
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        order.refreshFulfillmentStatus();
        if (statusChanged) {
            notificationEventPublisher.publish(NotificationEvent.fulfillmentChanged(
                order.getMember().getId(),
                order.getId(),
                request.fulfillmentStatus()
            ));
        }
        return toResponse(order, profile.getId());
    }

    private SellerOrderResponse toResponse(Order order, Long sellerProfileId) {
        List<SellerOrderItemResponse> items = ownedItems(order, sellerProfileId).stream()
            .map(this::toItemResponse)
            .toList();
        BigDecimal sellerAmount = items.stream()
            .map(SellerOrderItemResponse::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SellerOrderResponse(
            order.getId(),
            order.getStatus(),
            sellerAmount,
            order.getCreatedAt(),
            items
        );
    }

    private List<OrderItem> ownedItems(Order order, Long sellerProfileId) {
        return order.getItems().stream()
            .filter(item -> item.getProduct().getSellerProfile() != null)
            .filter(item -> item.getProduct().getSellerProfile().getId().equals(sellerProfileId))
            .toList();
    }

    private SellerOrderItemResponse toItemResponse(OrderItem item) {
        return new SellerOrderItemResponse(
            item.getId(),
            item.getProduct().getId(),
            item.getProductName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal(),
            item.getEffectiveFulfillmentStatus()
        );
    }
}
