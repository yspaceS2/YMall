package com.ymall.backend.notification.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.notification.entity.OrderEventProcessingResult;
import com.ymall.backend.notification.entity.ProcessedOrderEvent;
import com.ymall.backend.notification.event.OrderNotificationEventMapper;
import com.ymall.backend.notification.repository.ProcessedOrderEventRepository;

@Service
@RequiredArgsConstructor
public class OrderNotificationEventProcessor {

    private final ProcessedOrderEventRepository processedEventRepository;
    private final OrderEventTransitionValidator transitionValidator;
    private final OrderNotificationEventMapper eventMapper;
    private final NotificationService notificationService;

    @Transactional
    public void process(OrderEventEnvelope event) {
        if (processedEventRepository.existsByEventId(event.eventId())) {
            return;
        }

        OrderEventType previousEventType = processedEventRepository
            .findFirstByOrderIdAndResultOrderByIdDesc(
                event.orderId(), OrderEventProcessingResult.ACCEPTED
            )
            .map(ProcessedOrderEvent::getEventType)
            .orElse(null);
        if (!transitionValidator.isAllowed(previousEventType, event.eventType())) {
            processedEventRepository.save(new ProcessedOrderEvent(
                event, OrderEventProcessingResult.REJECTED, Instant.now()
            ));
            return;
        }

        notificationService.create(eventMapper.map(event));
        processedEventRepository.save(new ProcessedOrderEvent(
            event, OrderEventProcessingResult.ACCEPTED, Instant.now()
        ));
    }
}
