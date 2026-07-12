package com.ymall.backend.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
