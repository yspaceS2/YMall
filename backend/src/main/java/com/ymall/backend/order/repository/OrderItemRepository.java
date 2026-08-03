package com.ymall.backend.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        select distinct item.product.sellerProfile.member.id
        from OrderItem item
        where item.order.id = :orderId
        """)
    List<Long> findDistinctSellerMemberIdsByOrderId(@Param("orderId") Long orderId);

    @EntityGraph(attributePaths = {"order", "product"})
    Optional<OrderItem> findByIdAndOrderMemberIdAndFulfillmentStatus(
        Long orderItemId,
        Long memberId,
        OrderItemFulfillmentStatus fulfillmentStatus
    );
}
