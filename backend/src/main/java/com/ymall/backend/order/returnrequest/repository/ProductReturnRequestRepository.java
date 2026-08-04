package com.ymall.backend.order.returnrequest.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.order.returnrequest.entity.ProductReturnRequest;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;

public interface ProductReturnRequestRepository
    extends JpaRepository<ProductReturnRequest, Long> {

    @EntityGraph(attributePaths = {
        "member",
        "orderItem",
        "orderItem.product",
        "orderItem.order"
    })
    List<ProductReturnRequest> findAllByOrderItemOrderIdAndMemberIdOrderByRequestedAtDesc(
        Long orderId,
        Long memberId
    );

    @EntityGraph(attributePaths = {
        "member",
        "orderItem",
        "orderItem.product",
        "orderItem.order"
    })
    @Query(
        value = """
            select request
            from ProductReturnRequest request
            join request.orderItem item
            join item.product product
            join request.member member
            where product.sellerProfile.id = :sellerProfileId
              and (:filterByStatus = false or request.status = :status)
              and (
                  :keyword = ''
                  or lower(item.productName) like lower(concat('%', :keyword, '%'))
                  or lower(member.name) like lower(concat('%', :keyword, '%'))
              )
            order by request.requestedAt desc
            """,
        countQuery = """
            select count(request)
            from ProductReturnRequest request
            join request.orderItem item
            join item.product product
            join request.member member
            where product.sellerProfile.id = :sellerProfileId
              and (:filterByStatus = false or request.status = :status)
              and (
                  :keyword = ''
                  or lower(item.productName) like lower(concat('%', :keyword, '%'))
                  or lower(member.name) like lower(concat('%', :keyword, '%'))
              )
            """
    )
    Page<ProductReturnRequest> searchSellerRequests(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("filterByStatus") boolean filterByStatus,
        @Param("status") ReturnRequestStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "member",
        "orderItem",
        "orderItem.product",
        "orderItem.order"
    })
    Optional<ProductReturnRequest> findByIdAndOrderItemProductSellerProfileId(
        Long returnRequestId,
        Long sellerProfileId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request
        from ProductReturnRequest request
        join fetch request.member member
        join fetch request.orderItem item
        join fetch item.product product
        join fetch product.sellerProfile profile
        join fetch item.order orders
        where request.id = :requestId
          and profile.id = :sellerProfileId
        """)
    Optional<ProductReturnRequest> findSellerRequestForUpdate(
        @Param("requestId") Long requestId,
        @Param("sellerProfileId") Long sellerProfileId
    );

    @Query("""
        select coalesce(sum(request.quantity), 0)
        from ProductReturnRequest request
        where request.orderItem.id = :orderItemId
          and request.status = :status
        """)
    int sumQuantityByOrderItemIdAndStatus(
        @Param("orderItemId") Long orderItemId,
        @Param("status") ReturnRequestStatus status
    );

    @Query("""
        select count(request)
        from ProductReturnRequest request
        where request.orderItem.product.sellerProfile.id = :sellerProfileId
          and request.status = :status
        """)
    long countBySellerProfileIdAndStatus(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("status") ReturnRequestStatus status
    );
}
