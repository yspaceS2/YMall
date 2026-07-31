package com.ymall.backend.order.returnrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestCreateRequest;
import com.ymall.backend.order.returnrequest.entity.ProductReturnRequest;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.order.returnrequest.repository.ProductReturnRequestRepository;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.notification.service.NotificationService;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.service.SellerProfileService;

class ReturnRequestTransactionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 30, 12, 0);
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-30T12:00:00Z"),
        ZoneOffset.UTC
    );

    private ProductReturnRequestRepository returnRequestRepository;
    private OrderRepository orderRepository;
    private ReturnRequestTransactionService service;

    @BeforeEach
    void setUp() {
        returnRequestRepository = mock(ProductReturnRequestRepository.class);
        orderRepository = mock(OrderRepository.class);
        service = new ReturnRequestTransactionService(
            returnRequestRepository,
            mock(PaymentRefundRepository.class),
            orderRepository,
            mock(SellerProfileService.class),
            mock(NotificationService.class),
            CLOCK
        );
    }

    @Test
    void createsReturnRequestWithinSevenDaysOfDelivery() {
        Member member = mock(Member.class);
        Product product = mock(Product.class);
        SellerProfile sellerProfile = mock(SellerProfile.class);
        Member sellerMember = mock(Member.class);
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(member.getName()).willReturn("테스트 구매자");
        given(product.getId()).willReturn(3L);
        given(product.getSellerProfile()).willReturn(sellerProfile);
        given(sellerProfile.getMember()).willReturn(sellerMember);
        given(sellerMember.getId()).willReturn(2L);
        given(order.getId()).willReturn(10L);
        given(order.getMember()).willReturn(member);
        given(order.getItems()).willReturn(List.of(item));
        given(item.getId()).willReturn(20L);
        given(item.getOrder()).willReturn(order);
        given(item.getProduct()).willReturn(product);
        given(item.getProductName()).willReturn("테스트 상품");
        given(item.getEffectiveFulfillmentStatus())
            .willReturn(OrderItemFulfillmentStatus.DELIVERED);
        given(item.getDeliveredAt()).willReturn(NOW.minusDays(6));
        given(item.getRefundableQuantity()).willReturn(2);
        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(returnRequestRepository.sumQuantityByOrderItemIdAndStatus(
            20L,
            ReturnRequestStatus.REQUESTED
        )).willReturn(0);
        given(returnRequestRepository.save(any(ProductReturnRequest.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(
            1L,
            10L,
            new ReturnRequestCreateRequest(20L, 1, "상품 불량")
        );

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.REQUESTED);
        assertThat(response.quantity()).isEqualTo(1);
        assertThat(response.returnDeadline()).isEqualTo(NOW.plusDays(1));
    }

    @Test
    void rejectsReturnRequestAfterSevenDayWindow() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(order.getItems()).willReturn(List.of(item));
        given(item.getId()).willReturn(20L);
        given(item.getEffectiveFulfillmentStatus())
            .willReturn(OrderItemFulfillmentStatus.DELIVERED);
        given(item.getDeliveredAt()).willReturn(NOW.minusDays(8));
        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));

        assertThatThrownBy(() -> service.create(
            1L,
            10L,
            new ReturnRequestCreateRequest(20L, 1, "단순 변심")
        ))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.RETURN_REQUEST_NOT_ALLOWED)
            );
    }

    @Test
    void rejectsQuantityAlreadyCoveredByPendingReturn() {
        Order order = mock(Order.class);
        OrderItem item = mock(OrderItem.class);
        given(order.getItems()).willReturn(List.of(item));
        given(item.getId()).willReturn(20L);
        given(item.getEffectiveFulfillmentStatus())
            .willReturn(OrderItemFulfillmentStatus.DELIVERED);
        given(item.getDeliveredAt()).willReturn(NOW.minusDays(1));
        given(item.getRefundableQuantity()).willReturn(2);
        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(returnRequestRepository.sumQuantityByOrderItemIdAndStatus(
            20L,
            ReturnRequestStatus.REQUESTED
        )).willReturn(1);

        assertThatThrownBy(() -> service.create(
            1L,
            10L,
            new ReturnRequestCreateRequest(20L, 2, "상품 불량")
        ))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.RETURN_REQUEST_QUANTITY_EXCEEDED)
            );
    }
}
