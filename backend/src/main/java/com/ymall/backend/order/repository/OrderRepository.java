package com.ymall.backend.order.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndMemberId(Long orderId, Long memberId);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.sellerProfile"})
    @Query("select orders from Order orders where orders.id = :orderId")
    Optional<Order> findByIdForSettlement(@Param("orderId") Long orderId);

    Page<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @Query("""
        select orders.member.id, count(orders.id)
        from Order orders
        where orders.member.id in :memberIds
        group by orders.member.id
        """)
    List<Object[]> countOrdersByMemberIds(@Param("memberIds") Collection<Long> memberIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select orders from Order orders
        where orders.status = :status
          and orders.inventoryReserved = true
          and orders.createdAt <= :cutoff
        order by orders.createdAt, orders.id
        """)
    List<Order> findPendingForExpiration(
        @Param("status") OrderStatus status,
        @Param("cutoff") LocalDateTime cutoff,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from Order orders where orders.id = :orderId and orders.member.id = :memberId")
    Optional<Order> findByIdAndMemberIdForUpdate(
        @Param("orderId") Long orderId,
        @Param("memberId") Long memberId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select orders from Order orders where orders.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.product"})
    @Query("select orders from Order orders where orders.paymentOrderId = :paymentOrderId")
    Optional<Order> findByPaymentOrderIdForUpdate(
        @Param("paymentOrderId") String paymentOrderId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.sellerProfile"})
    @Query("""
        select orders from Order orders
        where orders.id = :orderId
          and exists (
              select item.id from OrderItem item
              where item.order = orders
                and item.product.sellerProfile.id = :sellerProfileId
          )
        """)
    Optional<Order> findSellerOrderByIdForUpdate(
        @Param("orderId") Long orderId,
        @Param("sellerProfileId") Long sellerProfileId
    );

}
