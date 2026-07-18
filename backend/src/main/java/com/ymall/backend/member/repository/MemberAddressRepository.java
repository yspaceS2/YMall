package com.ymall.backend.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.member.entity.MemberAddress;

public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {
    List<MemberAddress> findAllByMemberIdOrderByIsDefaultDescCreatedAtAsc(Long memberId);
    Optional<MemberAddress> findByIdAndMemberId(Long addressId, Long memberId);
    Optional<MemberAddress> findFirstByMemberIdAndIsDefaultTrue(Long memberId);
    boolean existsByMemberId(Long memberId);
}
