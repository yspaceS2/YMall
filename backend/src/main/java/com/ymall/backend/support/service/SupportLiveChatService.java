package com.ymall.backend.support.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.support.entity.SupportChatInitiator;
import com.ymall.backend.support.entity.SupportChatSession;
import com.ymall.backend.support.entity.SupportChatStatus;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportMessage;
import com.ymall.backend.support.entity.SupportMessageType;
import com.ymall.backend.support.repository.SupportChatSessionRepository;
import com.ymall.backend.support.repository.SupportMessageRepository;

@Service
@RequiredArgsConstructor
class SupportLiveChatService {

    private static final int LIVE_OFFER_MINUTES = 15;

    private final SupportChatSessionRepository chatSessionRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportInquiryAccessService inquiryAccessService;
    private final SupportEventService eventService;

    SupportInquiry requestLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryAccessService.getOwnedInquiry(principal.memberId(), inquiryId);
        inquiryAccessService.validateCanStartLive(inquiry);
        renewSession(inquiry, null, SupportChatInitiator.USER_REQUEST);
        inquiry.markLiveRequested();
        addSystemMessage(inquiry, inquiryAccessService.getMember(principal.memberId()), "실시간 상담을 요청했습니다.");
        eventService.notifyAdmins(
            inquiry,
            NotificationType.SUPPORT_LIVE_REQUESTED,
            "실시간 상담 요청이 도착했습니다",
            "고객센터 문의 #%d의 상담 요청을 확인해 주세요.".formatted(inquiryId)
        );
        publish(inquiry, "SUPPORT_LIVE_REQUESTED");
        return inquiry;
    }

    SupportInquiry offerLive(MemberPrincipal principal, Long inquiryId) {
        inquiryAccessService.requireAdmin(principal);
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        inquiryAccessService.validateCanStartLive(inquiry);
        Member admin = inquiryAccessService.getMember(principal.memberId());
        inquiry.assign(admin);
        renewSession(inquiry, admin, SupportChatInitiator.ADMIN_OFFER);
        inquiry.markLiveOffered();
        addSystemMessage(inquiry, admin, "관리자가 실시간 상담을 제안했습니다.");
        eventService.notifyRequester(
            inquiry,
            NotificationType.SUPPORT_LIVE_OFFERED,
            "실시간 상담 제안이 도착했습니다",
            "문의 #%d에서 관리자가 실시간 상담을 제안했습니다.".formatted(inquiryId)
        );
        publish(inquiry, "SUPPORT_LIVE_OFFERED");
        return inquiry;
    }

    SupportInquiry acceptLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        SupportChatSession session = getWaitingSession(inquiryId);
        boolean adminAcceptingRequest = session.getInitiatedBy() == SupportChatInitiator.USER_REQUEST
            && principal.role() == MemberRole.ROLE_ADMIN;
        boolean ownerAcceptingOffer = session.getInitiatedBy() == SupportChatInitiator.ADMIN_OFFER
            && principal.role() != MemberRole.ROLE_ADMIN
            && inquiry.getMember().getId().equals(principal.memberId());
        if (!adminAcceptingRequest && !ownerAcceptingOffer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Member admin = adminAcceptingRequest
            ? inquiryAccessService.getMember(principal.memberId())
            : session.getAdmin();
        inquiry.assign(admin);
        inquiry.markLiveActive();
        session.accept(admin);
        addSystemMessage(
            inquiry,
            inquiryAccessService.getMember(principal.memberId()),
            "실시간 상담이 시작되었습니다."
        );
        eventService.notifyRequester(
            inquiry,
            NotificationType.SUPPORT_LIVE_STATUS,
            "실시간 상담이 시작되었습니다",
            "문의 #%d의 상담 화면을 확인해 주세요.".formatted(inquiryId)
        );
        publish(inquiry, "SUPPORT_LIVE_ACTIVE");
        return inquiry;
    }

    SupportInquiry rejectLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        SupportChatSession session = getWaitingSession(inquiryId);
        boolean adminRejectingRequest = session.getInitiatedBy() == SupportChatInitiator.USER_REQUEST
            && principal.role() == MemberRole.ROLE_ADMIN;
        boolean ownerRejectingOffer = session.getInitiatedBy() == SupportChatInitiator.ADMIN_OFFER
            && principal.role() != MemberRole.ROLE_ADMIN
            && inquiry.getMember().getId().equals(principal.memberId());
        if (!adminRejectingRequest && !ownerRejectingOffer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        session.reject();
        inquiry.resumeGeneralInquiry();
        addSystemMessage(
            inquiry,
            inquiryAccessService.getMember(principal.memberId()),
            "실시간 상담 요청이 거절되었습니다."
        );
        publish(inquiry, "SUPPORT_LIVE_REJECTED");
        return inquiry;
    }

    SupportInquiry cancelLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        SupportChatSession session = getWaitingSession(inquiryId);
        boolean ownerCancelingRequest = session.getInitiatedBy() == SupportChatInitiator.USER_REQUEST
            && principal.role() != MemberRole.ROLE_ADMIN
            && inquiry.getMember().getId().equals(principal.memberId());
        boolean adminCancelingOffer = session.getInitiatedBy() == SupportChatInitiator.ADMIN_OFFER
            && principal.role() == MemberRole.ROLE_ADMIN;
        if (!ownerCancelingRequest && !adminCancelingOffer) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        session.reject();
        inquiry.resumeGeneralInquiry();
        addSystemMessage(
            inquiry,
            inquiryAccessService.getMember(principal.memberId()),
            "실시간 상담 요청이 취소되었습니다."
        );
        publish(inquiry, "SUPPORT_LIVE_CANCELED");
        return inquiry;
    }

    int expireWaitingSessions() {
        List<SupportChatSession> sessions = chatSessionRepository.findByStatusAndExpiresAtBefore(
            SupportChatStatus.WAITING,
            LocalDateTime.now()
        );
        sessions.forEach(session -> {
            SupportInquiry inquiry = session.getInquiry();
            session.expire();
            inquiry.resumeGeneralInquiry();
            addSystemMessage(inquiry, inquiry.getMember(), "실시간 상담 요청이 만료되었습니다.");
            publish(inquiry, "SUPPORT_LIVE_EXPIRED");
        });
        return sessions.size();
    }

    SupportInquiry endLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        SupportChatSession session = chatSessionRepository.findByInquiryId(inquiryId)
            .filter(candidate -> candidate.getStatus() == SupportChatStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID));
        session.end();
        inquiry.resumeGeneralInquiry();
        addSystemMessage(
            inquiry,
            inquiryAccessService.getMember(principal.memberId()),
            "실시간 상담이 종료되었습니다."
        );
        publish(inquiry, "SUPPORT_LIVE_ENDED");
        return inquiry;
    }

    private SupportChatSession renewSession(
        SupportInquiry inquiry,
        Member admin,
        SupportChatInitiator initiator
    ) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(LIVE_OFFER_MINUTES);
        return chatSessionRepository.findByInquiryId(inquiry.getId())
            .map(session -> {
                session.renew(admin, initiator, expiresAt);
                return session;
            })
            .orElseGet(() -> chatSessionRepository.save(
                new SupportChatSession(inquiry, admin, initiator, expiresAt)
            ));
    }

    private SupportChatSession getWaitingSession(Long inquiryId) {
        SupportChatSession session = chatSessionRepository.findByInquiryId(inquiryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_CHAT_NOT_FOUND));
        if (session.getStatus() != SupportChatStatus.WAITING
            || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID);
        }
        return session;
    }

    private void addSystemMessage(SupportInquiry inquiry, Member author, String content) {
        messageRepository.save(new SupportMessage(
            inquiry,
            author,
            SupportMessageType.SYSTEM,
            content,
            UUID.randomUUID()
        ));
    }

    private void publish(SupportInquiry inquiry, String type) {
        eventService.publishChanged(inquiry, type);
        eventService.publishInquiry(inquiry.getId());
    }
}
