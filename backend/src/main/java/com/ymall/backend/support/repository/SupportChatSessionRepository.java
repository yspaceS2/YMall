package com.ymall.backend.support.repository;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.support.entity.SupportChatSession;
import com.ymall.backend.support.entity.SupportChatStatus;

public interface SupportChatSessionRepository extends JpaRepository<SupportChatSession, Long> {

    @EntityGraph(attributePaths = "admin")
    Optional<SupportChatSession> findByInquiryId(Long inquiryId);

    @EntityGraph(attributePaths = {"admin", "inquiry", "inquiry.member"})
    List<SupportChatSession> findByStatusAndExpiresAtBefore(
        SupportChatStatus status,
        LocalDateTime expiresAt
    );
}
