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
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
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

    @Query(
        value = """
            select orders from Order orders
            where exists (
                select item.id from OrderItem item
                where item.order = orders
                  and item.product.sellerProfile.id = :sellerProfileId
                  and item.fulfillmentStatus = :fulfillmentStatus
                  and item.refundedQuantity < item.quantity
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
                  and item.fulfillmentStatus = :fulfillmentStatus
                  and item.refundedQuantity < item.quantity
            )
              and orders.status in :statuses
            """
    )
    Page<Order> findSellerOrdersByFulfillmentStatus(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses,
        @Param("fulfillmentStatus") OrderItemFulfillmentStatus fulfillmentStatus,
        Pageable pageable
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
              and (
                  :keyword = ''
                  or orders.id = :orderId
                  or exists (
                      select keywordItem.id from OrderItem keywordItem
                      where keywordItem.order = orders
                        and keywordItem.product.sellerProfile.id = :sellerProfileId
                        and lower(keywordItem.productName) like lower(concat('%', :keyword, '%'))
                  )
              )
              and (
                  :filterFulfillmentStatus = false
                  or exists (
                      select filteredItem.id from OrderItem filteredItem
                      where filteredItem.order = orders
                        and filteredItem.product.sellerProfile.id = :sellerProfileId
                        and filteredItem.fulfillmentStatus = :fulfillmentStatus
                        and filteredItem.refundedQuantity < filteredItem.quantity
                  )
              )
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
              and (
                  :keyword = ''
                  or orders.id = :orderId
                  or exists (
                      select keywordItem.id from OrderItem keywordItem
                      where keywordItem.order = orders
                        and keywordItem.product.sellerProfile.id = :sellerProfileId
                        and lower(keywordItem.productName) like lower(concat('%', :keyword, '%'))
                  )
              )
              and (
                  :filterFulfillmentStatus = false
                  or exists (
                      select filteredItem.id from OrderItem filteredItem
                      where filteredItem.order = orders
                        and filteredItem.product.sellerProfile.id = :sellerProfileId
                        and filteredItem.fulfillmentStatus = :fulfillmentStatus
                        and filteredItem.refundedQuantity < filteredItem.quantity
                  )
              )
            """
    )
    Page<Order> searchSellerOrders(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses,
        @Param("keyword") String keyword,
        @Param("orderId") Long orderId,
        @Param("filterFulfillmentStatus") boolean filterFulfillmentStatus,
        @Param("fulfillmentStatus") OrderItemFulfillmentStatus fulfillmentStatus,
        Pageable pageable
    );

    @Query("""
        select count(item) from OrderItem item
        where item.product.sellerProfile.id = :sellerProfileId
          and (
              item.fulfillmentStatus is null
              or item.fulfillmentStatus = com.ymall.backend.order.entity.OrderItemFulfillmentStatus.PENDING
          )
          and item.refundedQuantity < item.quantity
          and item.order.status in :statuses
        """)
    long countSellerPendingFulfillmentItems(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses
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
    Optional<Order> findSellerOrderById(
        @Param("orderId") Long orderId,
        @Param("sellerProfileId") Long sellerProfileId
    );
}
