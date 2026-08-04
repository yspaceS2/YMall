package com.ymall.backend.settlement.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;

public interface SettlementRequestRepository extends
    JpaRepository<SettlementRequest, Long>,
    JpaSpecificationExecutor<SettlementRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SettlementRequest request where request.id = :requestId")
    Optional<SettlementRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    @Override
    @EntityGraph(attributePaths = "sellerProfile")
    Page<SettlementRequest> findAll(
        Specification<SettlementRequest> specification,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "sellerProfile")
    Optional<SettlementRequest> findByIdAndSellerProfileId(
        Long requestId,
        Long sellerProfileId
    );

    long countBySellerProfileIdAndStatus(
        Long sellerProfileId,
        SettlementRequestStatus status
    );

}
