package com.ymall.backend.seller.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.ymall.backend.seller.entity.SellerProfile;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    @Override
    @EntityGraph(attributePaths = "member")
    Page<SellerProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "member")
    Optional<SellerProfile> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SellerProfile> findForUpdateByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    boolean existsByBusinessNumber(String businessNumber);
}
