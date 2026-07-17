package com.ymall.backend.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"order", "product"})
    Optional<OrderItem> findByIdAndOrderMemberIdAndFulfillmentStatus(
        Long orderItemId,
        Long memberId,
        OrderItemFulfillmentStatus fulfillmentStatus
    );
}
