package com.ymall.backend.seller.service;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.seller.dto.SellerDeliveryAddressResponse;
import com.ymall.backend.seller.dto.SellerOrderDetailResponse;
import com.ymall.backend.seller.dto.SellerOrderItemFulfillmentUpdateRequest;
import com.ymall.backend.seller.dto.SellerOrderItemResponse;
import com.ymall.backend.seller.dto.SellerOrderResponse;
import com.ymall.backend.seller.dto.SellerOrderStatusUpdateRequest;
import com.ymall.backend.seller.dto.SellerPendingOrderCountResponse;
import com.ymall.backend.seller.entity.SellerProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final EnumSet<OrderStatus> SELLER_VISIBLE_STATUSES = EnumSet.of(
        OrderStatus.PAID,
        OrderStatus.PARTIALLY_REFUNDED,
        OrderStatus.REFUNDED,
        OrderStatus.PREPARING,
        OrderStatus.SHIPPED,
        OrderStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SellerProfileService sellerProfileService;
    private final OrderOutboxService orderOutboxService;

    public PageResponse<SellerOrderResponse> getOrders(
        Long memberId,
        int page,
        int size,
        String keyword,
        OrderItemFulfillmentStatus fulfillmentStatus
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Long orderId = parseOrderId(normalizedKeyword);
        Page<Order> orders = orderRepository.searchSellerOrders(
            profile.getId(),
            SELLER_VISIBLE_STATUSES,
            normalizedKeyword,
            orderId,
            fulfillmentStatus != null,
            fulfillmentStatus == null ? OrderItemFulfillmentStatus.PENDING : fulfillmentStatus,
            pageable
        );
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Set<Long> refundSupportedOrderIds = orderIds.isEmpty()
            ? Set.of()
            : paymentRepository.findRefundSupportedOrderIds(
                orderIds,
                PaymentResult.SUCCESS
            );
        return PageResponse.from(orders.map(order -> toResponse(
            order,
            profile.getId(),
            refundSupportedOrderIds.contains(order.getId())
        )));
    }

    public SellerPendingOrderCountResponse getPendingOrderCount(Long memberId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        return new SellerPendingOrderCountResponse(
            orderRepository.countSellerPendingFulfillmentOrders(
                profile.getId(),
                SELLER_VISIBLE_STATUSES
            )
        );
    }

    public SellerOrderDetailResponse getOrder(Long memberId, Long orderId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Order order = orderRepository.findSellerOrderById(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        boolean refundSupported = paymentRepository
            .existsByOrderIdAndResultAndPaymentKeyIsNotNull(
                order.getId(),
                PaymentResult.SUCCESS
            );
        List<SellerOrderItemResponse> items = toItemResponses(order, profile.getId());
        return new SellerOrderDetailResponse(
            order.getId(),
            order.getStatus(),
            sellerAmount(items),
            order.getCreatedAt(),
            refundSupported,
            SellerDeliveryAddressResponse.from(order.getDeliveryAddress()),
            items
        );
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
        if (order.getStatus() != OrderStatus.PAID
            && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED
            && order.getStatus() != OrderStatus.PREPARING
            && order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }

        List<OrderItem> sellerItems = ownedItems(order, profile.getId()).stream()
            .filter(item -> item.getRefundableQuantity() > 0)
            .toList();
        if (sellerItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        OrderStatus previousOrderStatus = order.getStatus();
        try {
            sellerItems.forEach(item ->
                item.updateFulfillmentStatus(
                    request.fulfillmentStatus(),
                    request.carrier(),
                    request.trackingNumber()
                )
            );
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        order.refreshFulfillmentStatus();
        if (order.getStatus() != previousOrderStatus) {
            orderOutboxService.save(
                toOrderEventType(order.getStatus()),
                order.getId(),
                order.getMember().getId(),
                Map.of(
                    "status", order.getStatus().name(),
                    "fulfillmentStatus", request.fulfillmentStatus().name()
                )
            );
        }
        return toResponse(
            order,
            profile.getId(),
            paymentRepository.existsByOrderIdAndResultAndPaymentKeyIsNotNull(
                order.getId(),
                PaymentResult.SUCCESS
            )
        );
    }

    @Transactional
    public SellerOrderDetailResponse updateItemStatus(
        Long memberId,
        Long orderId,
        Long orderItemId,
        SellerOrderItemFulfillmentUpdateRequest request
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Order order = orderRepository.findSellerOrderByIdForUpdate(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        if (!SELLER_VISIBLE_STATUSES.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        OrderItem item = ownedItems(order, profile.getId()).stream()
            .filter(candidate -> candidate.getId().equals(orderItemId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        if (item.getRefundableQuantity() < 1) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        OrderItemFulfillmentStatus previousStatus = item.getEffectiveFulfillmentStatus();
        try {
            item.updateFulfillmentStatus(
                request.fulfillmentStatus(),
                request.carrier(),
                request.trackingNumber()
            );
        } catch (IllegalStateException exception) {
            throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        }
        order.refreshFulfillmentStatus();
        if (previousStatus != item.getEffectiveFulfillmentStatus()) {
            orderOutboxService.save(
                toOrderEventType(request.fulfillmentStatus()),
                order.getId(),
                order.getMember().getId(),
                Map.of(
                    "orderItemId", item.getId(),
                    "productName", item.getProductName(),
                    "fulfillmentStatus", request.fulfillmentStatus().name()
                )
            );
        }
        List<SellerOrderItemResponse> items = toItemResponses(order, profile.getId());
        return new SellerOrderDetailResponse(
            order.getId(),
            order.getStatus(),
            sellerAmount(items),
            order.getCreatedAt(),
            paymentRepository.existsByOrderIdAndResultAndPaymentKeyIsNotNull(
                order.getId(),
                PaymentResult.SUCCESS
            ),
            SellerDeliveryAddressResponse.from(order.getDeliveryAddress()),
            items
        );
    }

    private OrderEventType toOrderEventType(OrderItemFulfillmentStatus status) {
        return switch (status) {
            case PREPARING -> OrderEventType.ORDER_PREPARING;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            default -> throw new BusinessException(ErrorCode.ORDER_FULFILLMENT_NOT_ALLOWED);
        };
    }

    private OrderEventType toOrderEventType(OrderStatus status) {
        return switch (status) {
            case PREPARING -> OrderEventType.ORDER_PREPARING;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            default -> throw new IllegalStateException("Unsupported fulfillment order status: " + status);
        };
    }

    private SellerOrderResponse toResponse(
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

    private Long parseOrderId(String keyword) {
        try {
            return keyword.isBlank() ? null : Long.valueOf(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
