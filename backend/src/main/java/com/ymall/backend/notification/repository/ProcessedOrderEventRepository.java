package com.ymall.backend.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.notification.entity.OrderEventProcessingResult;
import com.ymall.backend.notification.entity.ProcessedOrderEvent;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<ProcessedOrderEvent> findByEventId(UUID eventId);

    Optional<ProcessedOrderEvent> findFirstByOrderIdAndResultOrderByIdDesc(
        Long orderId,
        OrderEventProcessingResult result
    );
}
