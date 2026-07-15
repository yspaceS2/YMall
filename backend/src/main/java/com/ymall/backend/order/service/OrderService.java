package com.ymall.backend.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
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

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(Long memberId, OrderCreateRequest request) {
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return orderRepository.findByMemberIdAndIdempotencyKey(memberId, request.idempotencyKey())
            .map(orderMapper::toOrderResponse)
            .orElseGet(() -> createNewOrder(member, request.idempotencyKey()));
    }

    private OrderResponse createNewOrder(Member member, String idempotencyKey) {
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

        Order order = new Order(member, idempotencyKey);
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
        return orderMapper.toOrderResponse(savedOrder);
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
