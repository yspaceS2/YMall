package com.ymall.backend.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByMemberId(Long memberId, Pageable pageable);

    Optional<Notification> findByIdAndMemberId(Long notificationId, Long memberId);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update Notification notification
        set notification.readAt = :readAt
        where notification.member.id = :memberId
          and notification.readAt is null
        """)
    int markAllAsRead(
        @Param("memberId") Long memberId,
        @Param("readAt") LocalDateTime readAt
    );
}
