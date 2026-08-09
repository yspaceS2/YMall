package com.ymall.backend.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.order.entity.Order;

public interface AdminOrderQueryRepository extends Repository<Order, Long> {

    @EntityGraph(attributePaths = {"member", "items", "items.product"})
    @Query("""
        select distinct orders from Order orders
        where (:keyword = ''
            or lower(orders.member.name) like lower(concat('%', :keyword, '%'))
            or lower(orders.member.email) like lower(concat('%', :keyword, '%'))
            or exists (
                select item.id from OrderItem item
                where item.order = orders
                  and lower(item.productName) like lower(concat('%', :keyword, '%'))
            )
        )
          and (:orderId is null or orders.id = :orderId)
          and (:filterPendingRefund = false or exists (
              select refund.id from PaymentRefund refund
              where refund.order = orders
                and refund.status = com.ymall.backend.payment.refund.entity.PaymentRefundStatus.PENDING
          ))
          and (:filterPendingReturn = false or exists (
              select returnRequest.id from ProductReturnRequest returnRequest
              where returnRequest.orderItem.order = orders
                and returnRequest.status = com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus.REQUESTED
          ))
        """)
    Page<Order> search(
        @Param("keyword") String keyword,
        @Param("orderId") Long orderId,
        @Param("filterPendingRefund") boolean filterPendingRefund,
        @Param("filterPendingReturn") boolean filterPendingReturn,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"member", "items", "items.product"})
    @Query("select orders from Order orders where orders.id = :orderId")
    Optional<Order> findById(@Param("orderId") Long orderId);
}
