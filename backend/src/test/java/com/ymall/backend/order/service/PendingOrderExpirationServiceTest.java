package com.ymall.backend.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.config.PendingOrderExpirationProperties;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.service.PaymentInventoryService;

@ExtendWith(MockitoExtension.class)
class PendingOrderExpirationServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentInventoryService paymentInventoryService;
    @Mock private OrderOutboxService orderOutboxService;

    @InjectMocks
    private PendingOrderExpirationService expirationService;

    @Test
    void cancelsExpiredPendingOrderAndReleasesInventory() {
        PendingOrderExpirationProperties properties =
            new PendingOrderExpirationProperties(Duration.ofMinutes(30), 100);
        expirationService = new PendingOrderExpirationService(
            orderRepository,
            paymentInventoryService,
            orderOutboxService,
            properties
        );
        Member member = new Member(
            "expired@example.com",
            "password",
            "만료 사용자",
            MemberRole.ROLE_USER
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        Order order = new Order(member, "expired-order");
        ReflectionTestUtils.setField(order, "id", 10L);
        LocalDateTime cutoff = LocalDateTime.of(2026, 7, 26, 10, 0);
        given(orderRepository.findPendingForExpiration(
            OrderStatus.PENDING_PAYMENT,
            cutoff,
            PageRequest.of(0, 100)
        )).willReturn(List.of(order));

        int expiredCount = expirationService.expireCreatedOnOrBefore(cutoff);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        then(paymentInventoryService).should().releaseIfReserved(order);
        then(orderOutboxService).should().save(
            eq(OrderEventType.ORDER_CANCELED),
            eq(10L),
            eq(1L),
            eq(java.util.Map.of(
                "status",
                OrderStatus.CANCELED.name(),
                "reason",
                PendingOrderExpirationService.EXPIRATION_REASON
            ))
        );
    }
}
