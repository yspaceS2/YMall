package com.ymall.backend.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.event.NotificationEventPublisher;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.DeliveryAddressSnapshot;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.mapper.OrderMapper;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_PAGE_SIZE = 100;

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final MemberAddressRepository memberAddressRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return orderRepository.findByMemberIdAndIdempotencyKey(memberId, request.idempotencyKey())
            .map(orderMapper::toOrderResponse)
            .orElseGet(() -> createNewOrder(member, request));
    }

    public OrderResponse getOrder(Long memberId, Long orderId) {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
            .map(orderMapper::toOrderResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    public PageResponse<OrderResponse> getOrders(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(
            orderRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(orderMapper::toOrderResponse)
        );
    }

    @Transactional
    public OrderResponse cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
            && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.ORDER_CANCELLATION_NOT_ALLOWED);
        }

        List<Long> productIds = order.getItems().stream()
            .map(item -> item.getProduct().getId())
            .sorted()
            .toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            product.increaseStock(item.getQuantity());
        }
        order.cancel();
        notificationEventPublisher.publish(NotificationEvent.orderCanceled(memberId, orderId));
        return orderMapper.toOrderResponse(order);
    }

    private OrderResponse createNewOrder(Member member, OrderCreateRequest request) {
        List<CartItem> cartItems = cartItemRepository.findAllByMemberIdForUpdate(member.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        List<Long> productIds = cartItems.stream()
            .map(cartItem -> cartItem.getProduct().getId())
            .sorted()
            .toList();
        Map<Long, Product> products = productRepository.findAllByIdForUpdate(productIds)
            .stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));

        DeliveryAddressSnapshot deliveryAddress = resolveDeliveryAddress(member.getId(), request.addressId());
        Order order = new Order(member, request.idempotencyKey(), deliveryAddress);
        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProduct().getId());
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            validateOrderable(product, cartItem.getQuantity());

            order.addItem(new OrderItem(
                product,
                product.getName(),
                calculateUnitPrice(product),
                cartItem.getQuantity()
            ));
            product.decreaseStock(cartItem.getQuantity());
        }

        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        notificationEventPublisher.publish(
            NotificationEvent.orderCreated(member.getId(), savedOrder.getId())
        );
        return orderMapper.toOrderResponse(savedOrder);
    }

    private DeliveryAddressSnapshot resolveDeliveryAddress(Long memberId, Long addressId) {
        if (addressId == null) {
            throw new BusinessException(ErrorCode.MEMBER_ADDRESS_NOT_FOUND);
        }
        MemberAddress address = memberAddressRepository.findByIdAndMemberId(addressId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_ADDRESS_NOT_FOUND));
        return new DeliveryAddressSnapshot(address);
    }

    private void validateOrderable(Product product, int quantity) {
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ORDERABLE);
        }
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private BigDecimal calculateUnitPrice(Product product) {
        BigDecimal discountPercentage = product.getDiscountPercentage() == null
            ? BigDecimal.ZERO
            : product.getDiscountPercentage();
        return product.getPrice()
            .multiply(ONE_HUNDRED.subtract(discountPercentage))
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
