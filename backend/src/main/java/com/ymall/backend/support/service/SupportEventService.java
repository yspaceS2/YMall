package com.ymall.backend.support.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.service.NotificationService;
import com.ymall.backend.realtime.dto.RealtimeEvent;
import com.ymall.backend.realtime.service.RealtimePublisher;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportRequesterType;

@Service
@RequiredArgsConstructor
class SupportEventService {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;

    void notifyAdmins(
        SupportInquiry inquiry,
        NotificationType type,
        String title,
        String message
    ) {
        memberRepository.findAllByRole(MemberRole.ROLE_ADMIN).stream()
            .filter(admin -> admin.getAdminGrade() != null
                && admin.getAdminGrade().hasPermission(AdminPermission.SUPPORT_REPLY))
            .forEach(admin -> notificationService.create(new NotificationEvent(
                UUID.randomUUID(),
                admin.getId(),
                type,
                title,
                message,
                "/admin/support/%d".formatted(inquiry.getId())
            )));
    }

    void notifyRequester(
        SupportInquiry inquiry,
        NotificationType type,
        String title,
        String message
    ) {
        String target = inquiry.getRequesterType() == SupportRequesterType.SELLER
            ? "/seller/support/%d".formatted(inquiry.getId())
            : "/mypage/support/%d".formatted(inquiry.getId());
        notificationService.create(new NotificationEvent(
            UUID.randomUUID(),
            inquiry.getMember().getId(),
            type,
            title,
            message,
            target
        ));
        realtimePublisher.publishToMember(
            inquiry.getMember().getId(),
            RealtimeEvent.of("NOTIFICATIONS_INVALIDATED", "notification", null)
        );
    }

    void publishChanged(SupportInquiry inquiry, String type) {
        RealtimeEvent event = RealtimeEvent.of(type, "supportInquiry", inquiry.getId());
        realtimePublisher.publishToAdmins(event);
        realtimePublisher.publishToMember(inquiry.getMember().getId(), event);
    }

    void publishInquiry(Long inquiryId) {
        realtimePublisher.publishInquiry(inquiryId);
    }
}
