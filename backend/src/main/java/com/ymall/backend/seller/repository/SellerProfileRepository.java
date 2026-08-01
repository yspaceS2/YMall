package com.ymall.backend.seller.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.seller.entity.SellerProfile;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    @Override
    @EntityGraph(attributePaths = "member")
    Page<SellerProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "member")
    Optional<SellerProfile> findByMemberId(Long memberId);

    @EntityGraph(attributePaths = "member")
    @Query("select profile from SellerProfile profile where profile.id = :sellerProfileId")
    Optional<SellerProfile> findWithMemberById(@Param("sellerProfileId") Long sellerProfileId);

    @EntityGraph(attributePaths = "member")
    @Query("""
        select profile from SellerProfile profile
        where lower(profile.storeName) like lower(concat('%', :keyword, '%'))
           or lower(profile.businessNumber) like lower(concat('%', :keyword, '%'))
           or lower(profile.member.name) like lower(concat('%', :keyword, '%'))
           or lower(profile.member.email) like lower(concat('%', :keyword, '%'))
        """)
    Page<SellerProfile> search(@Param("keyword") String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SellerProfile> findForUpdateByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    boolean existsByBusinessNumber(String businessNumber);
}
