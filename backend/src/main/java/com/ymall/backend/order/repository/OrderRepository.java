package com.ymall.backend.order.repository;

import java.util.Collection;
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

    Page<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from Order orders where orders.id = :orderId and orders.member.id = :memberId")
    Optional<Order> findByIdAndMemberIdForUpdate(
        @Param("orderId") Long orderId,
        @Param("memberId") Long memberId
    );

    @Query(
        value = """
            select orders from Order orders
            where exists (
                select item.id from OrderItem item
                where item.order = orders
                  and item.product.sellerProfile.id = :sellerProfileId
            )
              and orders.status in :statuses
            order by orders.createdAt desc
            """,
        countQuery = """
            select count(orders) from Order orders
            where exists (
                select item.id from OrderItem item
                where item.order = orders
                  and item.product.sellerProfile.id = :sellerProfileId
            )
              and orders.status in :statuses
            """
    )
    Page<Order> findSellerOrders(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses,
        Pageable pageable
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
