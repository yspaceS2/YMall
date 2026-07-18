package com.ymall.backend.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.event.NotificationEventPublisher;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.mapper.OrderMapper;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberAddressRepository memberAddressRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createsOrderFromCartAndDecreasesStock() {
        Member member = member();
        Product product = product(10);
        CartItem cartItem = new CartItem(member, product, 3);
        OrderResponse response = response(1L);

        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(orderRepository.findByMemberIdAndIdempotencyKey(1L, "request-1"))
            .willReturn(Optional.empty());
        given(memberAddressRepository.findByIdAndMemberId(1L, 1L))
            .willReturn(Optional.of(address(member)));
        given(cartItemRepository.findAllByMemberIdForUpdate(1L)).willReturn(List.of(cartItem));
        given(productRepository.findAllByIdForUpdate(List.of(1L))).willReturn(List.of(product));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderMapper.toOrderResponse(any(Order.class))).willReturn(response);

        OrderResponse result = orderService.createOrder(1L, new OrderCreateRequest("request-1", 1L));

        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(product.getStock()).isEqualTo(7);
        then(cartItemRepository).should().deleteAll(List.of(cartItem));
        then(orderRepository).should().save(any(Order.class));
    }

    @Test
    void returnsExistingOrderForDuplicateIdempotencyKey() {
        Member member = member();
        Order existingOrder = new Order(member, "request-1");
        OrderResponse response = response(10L);

        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(orderRepository.findByMemberIdAndIdempotencyKey(1L, "request-1"))
            .willReturn(Optional.of(existingOrder));
        given(orderMapper.toOrderResponse(existingOrder)).willReturn(response);

        OrderResponse result = orderService.createOrder(1L, new OrderCreateRequest("request-1", 1L));

        assertThat(result.orderId()).isEqualTo(10L);
        then(cartItemRepository).shouldHaveNoInteractions();
        then(productRepository).shouldHaveNoInteractions();
        then(orderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void rejectsOrderWhenStockIsInsufficient() {
        Member member = member();
        Product product = product(1);
        CartItem cartItem = new CartItem(member, product, 2);

        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(orderRepository.findByMemberIdAndIdempotencyKey(1L, "request-1"))
            .willReturn(Optional.empty());
        given(memberAddressRepository.findByIdAndMemberId(1L, 1L))
            .willReturn(Optional.of(address(member)));
        given(cartItemRepository.findAllByMemberIdForUpdate(1L)).willReturn(List.of(cartItem));
        given(productRepository.findAllByIdForUpdate(List.of(1L))).willReturn(List.of(product));

        assertThatThrownBy(() -> orderService.createOrder(1L, new OrderCreateRequest("request-1", 1L)))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);

        assertThat(product.getStock()).isEqualTo(1);
        then(orderRepository).shouldHaveNoMoreInteractions();
        then(cartItemRepository).shouldHaveNoMoreInteractions();
    }

    private Member member() {
        Member member = new Member("user@example.com", "password", "홍길동", MemberRole.ROLE_USER);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Product product(int stock) {
        Product product = new Product(
            new Category("전자기기", "electronics"),
            "무선 키보드",
            "description",
            "YMall",
            BigDecimal.valueOf(39000),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.5),
            stock,
            "thumbnail",
            ProductStatus.APPROVED
        );
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }

    private MemberAddress address(Member member) {
        return new MemberAddress(
            member,
            "Home",
            "Recipient",
            "01012345678",
            "12159",
            "186 Biryong-ro",
            "101",
            true
        );
    }

    private OrderResponse response(Long orderId) {
        return new OrderResponse(
            orderId,
            OrderStatus.PENDING_PAYMENT,
            BigDecimal.valueOf(105300),
            List.of(),
            LocalDateTime.now()
        );
    }
}
