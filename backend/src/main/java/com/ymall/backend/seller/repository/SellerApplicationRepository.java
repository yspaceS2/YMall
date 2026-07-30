package com.ymall.backend.seller.repository;

import java.util.Collection;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.seller.entity.SellerApplicationStatus;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    Optional<SellerApplication> findByMemberId(Long memberId);

    boolean existsByBusinessNumberAndStatusIn(
        String businessNumber,
        Collection<SellerApplicationStatus> statuses
    );

    boolean existsByBusinessNumberAndStatusInAndIdNot(
        String businessNumber,
        Collection<SellerApplicationStatus> statuses,
        Long id
    );

    @EntityGraph(attributePaths = "member")
    Page<SellerApplication> findByStatus(
        SellerApplicationStatus status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "member")
    @Query("""
        select application from SellerApplication application
        where application.status = :status
          and (
              lower(application.storeName) like lower(concat('%', :keyword, '%'))
              or lower(application.businessNumber) like lower(concat('%', :keyword, '%'))
              or lower(application.member.name) like lower(concat('%', :keyword, '%'))
              or lower(application.member.email) like lower(concat('%', :keyword, '%'))
          )
        """)
    Page<SellerApplication> searchByStatus(
        @Param("status") SellerApplicationStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "member")
    @Query("select application from SellerApplication application where application.id = :applicationId")
    Optional<SellerApplication> findWithMemberById(@Param("applicationId") Long applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from SellerApplication application where application.id = :id")
    Optional<SellerApplication> findByIdForUpdate(@Param("id") Long id);
}
