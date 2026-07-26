package com.ymall.backend.order.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.config.PendingOrderExpirationProperties;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.service.PaymentInventoryService;

@Service
@RequiredArgsConstructor
public class PendingOrderExpirationService {

    static final String EXPIRATION_REASON = "PAYMENT_TIMEOUT";

    private final OrderRepository orderRepository;
    private final PaymentInventoryService paymentInventoryService;
    private final OrderOutboxService orderOutboxService;
    private final PendingOrderExpirationProperties properties;

    @Transactional
    public int expireCreatedOnOrBefore(LocalDateTime cutoff) {
        var orders = orderRepository.findPendingForExpiration(
            OrderStatus.PENDING_PAYMENT,
            cutoff,
            PageRequest.of(0, properties.batchSize())
        );

        for (Order order : orders) {
            paymentInventoryService.releaseIfReserved(order);
            order.cancel();
            orderOutboxService.save(
                OrderEventType.ORDER_CANCELED,
                order.getId(),
                order.getMember().getId(),
                Map.of(
                    "status", OrderStatus.CANCELED.name(),
                    "reason", EXPIRATION_REASON
                )
            );
        }
        return orders.size();
    }
}
