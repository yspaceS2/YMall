package com.ymall.backend.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Member> findByEmailIgnoreCase(String email);

    java.util.List<Member> findAllByRole(MemberRole role);

    @Query("""
        select member from Member member
        where lower(member.name) like lower(concat('%', :keyword, '%'))
           or lower(member.email) like lower(concat('%', :keyword, '%'))
        """)
    Page<Member> search(@Param("keyword") String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from Member member where member.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") Long memberId);
}
