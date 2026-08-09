package com.ymall.backend.order.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;

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

    @Query("""
        select count(distinct item.order.id)
        from OrderItem item
        where item.product.sellerProfile.id = :sellerProfileId
          and item.order.status in :statuses
        """)
    long countSellerOrders(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses
    );

    @Query("""
        select coalesce(sum(item.lineTotal), 0)
        from OrderItem item
        where item.product.sellerProfile.id = :sellerProfileId
          and item.order.status in :statuses
        """)
    BigDecimal sumSellerGrossSales(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses
    );

    @Query("""
        select coalesce(sum(item.refundedQuantity), 0)
        from OrderItem item
        where item.product.sellerProfile.id = :sellerProfileId
        """)
    long sumSellerRefundedQuantity(@Param("sellerProfileId") Long sellerProfileId);
}
