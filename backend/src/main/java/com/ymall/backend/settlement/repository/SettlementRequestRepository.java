package com.ymall.backend.settlement.repository;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;

public interface SettlementRequestRepository extends JpaRepository<SettlementRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SettlementRequest request where request.id = :requestId")
    Optional<SettlementRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    @EntityGraph(attributePaths = "sellerProfile")
    @Query("""
        select request from SettlementRequest request
        where request.sellerProfile.id = :sellerProfileId
          and (:status is null or request.status = :status)
          and (:requestId is null or request.id = :requestId)
          and (:requestedFrom is null or request.createdAt >= :requestedFrom)
          and (:requestedToExclusive is null or request.createdAt < :requestedToExclusive)
        order by request.createdAt desc, request.id desc
        """)
    Page<SettlementRequest> findSellerRequests(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("status") SettlementRequestStatus status,
        @Param("requestId") Long requestId,
        @Param("requestedFrom") Instant requestedFrom,
        @Param("requestedToExclusive") Instant requestedToExclusive,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "sellerProfile")
    Optional<SettlementRequest> findByIdAndSellerProfileId(
        Long requestId,
        Long sellerProfileId
    );

    @EntityGraph(attributePaths = "sellerProfile")
    @Query("""
        select request from SettlementRequest request
        where (:status is null or request.status = :status)
          and (:requestId is null or request.id = :requestId)
          and (
            :sellerKeyword is null
            or lower(request.sellerProfile.storeName) like lower(
                concat('%', :sellerKeyword, '%')
            )
          )
          and (:requestedFrom is null or request.createdAt >= :requestedFrom)
          and (:requestedToExclusive is null or request.createdAt < :requestedToExclusive)
        order by request.createdAt desc, request.id desc
        """)
    Page<SettlementRequest> findAdminRequests(
        @Param("status") SettlementRequestStatus status,
        @Param("requestId") Long requestId,
        @Param("sellerKeyword") String sellerKeyword,
        @Param("requestedFrom") Instant requestedFrom,
        @Param("requestedToExclusive") Instant requestedToExclusive,
        Pageable pageable
    );
}
