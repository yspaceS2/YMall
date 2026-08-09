package com.ymall.backend.order.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;

public interface SellerOrderQueryRepository extends Repository<Order, Long> {

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
                        and (
                            filteredItem.fulfillmentStatus in :fulfillmentStatuses
                            or (:includeLegacyPending = true and filteredItem.fulfillmentStatus is null)
                        )
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
                        and (
                            filteredItem.fulfillmentStatus in :fulfillmentStatuses
                            or (:includeLegacyPending = true and filteredItem.fulfillmentStatus is null)
                        )
                        and filteredItem.refundedQuantity < filteredItem.quantity
                  )
              )
            """
    )
    Page<Order> search(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses,
        @Param("keyword") String keyword,
        @Param("orderId") Long orderId,
        @Param("filterFulfillmentStatus") boolean filterFulfillmentStatus,
        @Param("fulfillmentStatuses") Collection<OrderItemFulfillmentStatus> fulfillmentStatuses,
        @Param("includeLegacyPending") boolean includeLegacyPending,
        Pageable pageable
    );

    @Query("""
        select count(distinct item.order.id) from OrderItem item
        where item.product.sellerProfile.id = :sellerProfileId
          and (
              item.fulfillmentStatus is null
              or item.fulfillmentStatus in (
                  com.ymall.backend.order.entity.OrderItemFulfillmentStatus.PENDING,
                  com.ymall.backend.order.entity.OrderItemFulfillmentStatus.PREPARING
              )
          )
          and item.refundedQuantity < item.quantity
          and item.order.status in :statuses
        """)
    long countActionRequired(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("statuses") Collection<OrderStatus> statuses
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
    Optional<Order> findById(
        @Param("orderId") Long orderId,
        @Param("sellerProfileId") Long sellerProfileId
    );
}
