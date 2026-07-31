package com.ymall.backend.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.dto.NotificationReadAllResponse;
import com.ymall.backend.notification.dto.NotificationResponse;
import com.ymall.backend.notification.dto.NotificationUnreadCountResponse;
import com.ymall.backend.notification.entity.Notification;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public PageResponse<NotificationResponse> getNotifications(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return PageResponse.from(
            notificationRepository.findByMemberId(memberId, pageable)
                .map(NotificationResponse::from)
        );
    }

    public NotificationUnreadCountResponse getUnreadCount(Long memberId) {
        return new NotificationUnreadCountResponse(
            notificationRepository.countByMemberIdAndReadAtIsNull(memberId)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository
            .findByIdAndMemberId(notificationId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationReadAllResponse markAllAsRead(Long memberId) {
        int updatedCount = notificationRepository.markAllAsRead(
            memberId,
            LocalDateTime.now(clock)
        );
        return new NotificationReadAllResponse(updatedCount);
    }

    @Transactional
    public void delete(Long memberId, Long notificationId) {
        validatePersonalNotificationDeletion(memberId);
        Notification notification = notificationRepository
            .findByIdAndMemberId(notificationId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(Long memberId) {
        validatePersonalNotificationDeletion(memberId);
        notificationRepository.deleteAllByMemberId(memberId);
    }

    @Transactional
    public void create(NotificationEvent event) {
        if (notificationRepository.existsBySourceEventId(event.sourceEventId())) {
            return;
        }
        Member member = memberRepository.findById(event.memberId())
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        notificationRepository.save(new Notification(
            member,
            event.sourceEventId(),
            event.type(),
            event.title(),
            event.message(),
            event.targetUrl()
        ));
    }

    private void validatePersonalNotificationDeletion(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole() != MemberRole.ROLE_USER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
