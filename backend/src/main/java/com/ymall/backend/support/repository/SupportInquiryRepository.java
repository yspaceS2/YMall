package com.ymall.backend.support.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import jakarta.persistence.LockModeType;

public interface SupportInquiryRepository extends JpaRepository<SupportInquiry, Long> {

    @EntityGraph(attributePaths = {"member", "assignedAdmin"})
    Page<SupportInquiry> findByMemberIdOrderByUpdatedAtDescIdDesc(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = {"member", "assignedAdmin"})
    Optional<SupportInquiry> findByIdAndMemberId(Long inquiryId, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"member", "assignedAdmin"})
    @Query("select inquiry from SupportInquiry inquiry where inquiry.id = :inquiryId")
    Optional<SupportInquiry> findByIdForUpdate(@Param("inquiryId") Long inquiryId);

    @EntityGraph(attributePaths = {"member", "assignedAdmin"})
    @Query("""
        select inquiry from SupportInquiry inquiry
        left join inquiry.assignedAdmin assignedAdmin
        where (:status is null or inquiry.status = :status)
          and (:keyword = ''
            or lower(inquiry.title) like lower(concat('%', :keyword, '%'))
            or lower(inquiry.member.name) like lower(concat('%', :keyword, '%'))
            or lower(assignedAdmin.name) like lower(concat('%', :keyword, '%')))
        order by inquiry.updatedAt desc, inquiry.id desc
        """)
    Page<SupportInquiry> searchAdmin(
        @Param("status") SupportInquiryStatus status,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    long countByStatusIn(Iterable<SupportInquiryStatus> statuses);

    long countByMemberIdAndStatusIn(Long memberId, Iterable<SupportInquiryStatus> statuses);
}
