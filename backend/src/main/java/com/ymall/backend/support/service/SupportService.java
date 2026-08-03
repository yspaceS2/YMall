package com.ymall.backend.support.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.notification.event.NotificationEvent;
import com.ymall.backend.notification.service.NotificationService;
import com.ymall.backend.realtime.dto.RealtimeEvent;
import com.ymall.backend.realtime.service.RealtimePublisher;
import com.ymall.backend.support.dto.SupportChatSessionResponse;
import com.ymall.backend.support.dto.SupportInquiryCreateRequest;
import com.ymall.backend.support.dto.SupportInquiryDetailResponse;
import com.ymall.backend.support.dto.SupportInquirySummaryResponse;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.dto.SupportMessageResponse;
import com.ymall.backend.support.dto.SupportPendingCountResponse;
import com.ymall.backend.support.dto.SupportResolutionRequest;
import com.ymall.backend.support.entity.SupportChatInitiator;
import com.ymall.backend.support.entity.SupportAttachment;
import com.ymall.backend.support.entity.SupportChatSession;
import com.ymall.backend.support.entity.SupportChatStatus;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.entity.SupportMessage;
import com.ymall.backend.support.entity.SupportMessageType;
import com.ymall.backend.support.entity.SupportRequesterType;
import com.ymall.backend.support.repository.SupportChatSessionRepository;
import com.ymall.backend.support.repository.SupportAttachmentRepository;
import com.ymall.backend.support.repository.SupportInquiryRepository;
import com.ymall.backend.support.repository.SupportMessageRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int LIVE_OFFER_MINUTES = 15;

    private final SupportInquiryRepository inquiryRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportAttachmentRepository attachmentRepository;
    private final SupportAttachmentStorageService attachmentStorageService;
    private final SupportChatSessionRepository chatSessionRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;

    public PageResponse<SupportInquirySummaryResponse> getMyInquiries(
        Long memberId,
        int page,
        int size
    ) {
        return PageResponse.from(
            inquiryRepository.findByMemberIdOrderByUpdatedAtDescIdDesc(
                memberId,
                pageRequest(page, size)
            ).map(SupportInquirySummaryResponse::from)
        );
    }

    public PageResponse<SupportInquirySummaryResponse> getAdminInquiries(
        int page,
        int size,
        SupportInquiryStatus status,
        String keyword
    ) {
        return PageResponse.from(
            inquiryRepository.searchAdmin(
                status,
                keyword == null ? "" : keyword.trim(),
                pageRequest(page, size)
            ).map(SupportInquirySummaryResponse::from)
        );
    }

    public SupportPendingCountResponse getAdminPendingCount() {
        return new SupportPendingCountResponse(inquiryRepository.countByStatusIn(List.of(
            SupportInquiryStatus.WAITING,
            SupportInquiryStatus.LIVE_REQUESTED
        )));
    }

    public SupportInquiryDetailResponse getInquiry(MemberPrincipal principal, Long inquiryId) {
        return toDetail(
            getAccessibleInquiry(principal, inquiryId),
            principal.role() == MemberRole.ROLE_ADMIN
        );
    }

    @Transactional
    public SupportInquiryDetailResponse create(
        MemberPrincipal principal,
        SupportInquiryCreateRequest request
    ) {
        if (principal.role() == MemberRole.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        Member member = getMember(principal.memberId());
        SupportInquiry inquiry = inquiryRepository.save(new SupportInquiry(
            member,
            principal.role() == MemberRole.ROLE_SELLER
                ? SupportRequesterType.SELLER
                : SupportRequesterType.CUSTOMER,
            request.category(),
            request.title().trim(),
            request.relatedOrderId(),
            request.relatedProductId(),
            request.relatedSettlementId()
        ));
        messageRepository.save(new SupportMessage(
            inquiry,
            member,
            SupportMessageType.INQUIRY,
            request.content().trim(),
            UUID.randomUUID()
        ));
        notifyAdmins(
            inquiry,
            NotificationType.SUPPORT_INQUIRY_CREATED,
            "새 고객센터 문의가 등록되었습니다",
            "%s님의 문의를 확인해 주세요.".formatted(member.getName())
        );
        publishChanged(inquiry, "SUPPORT_INQUIRY_CREATED");
        return toDetail(inquiry);
    }

    @Transactional
    public SupportMessageResponse addMessage(
        MemberPrincipal principal,
        Long inquiryId,
        SupportMessageCreateRequest request,
        boolean liveMessage
    ) {
        SupportInquiry inquiry = getAccessibleInquiryForUpdate(principal, inquiryId);
        validateWritable(inquiry);
        if (liveMessage && inquiry.getStatus() != SupportInquiryStatus.LIVE_ACTIVE) {
            throw new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID);
        }
        if (!liveMessage && (
            inquiry.getStatus() == SupportInquiryStatus.LIVE_REQUESTED
                || inquiry.getStatus() == SupportInquiryStatus.LIVE_OFFERED
                || inquiry.getStatus() == SupportInquiryStatus.LIVE_ACTIVE
        )) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_STATUS_INVALID);
        }
        return messageRepository.findByInquiryIdAndClientMessageId(
            inquiryId,
            request.clientMessageId()
        ).map(SupportMessageResponse::from).orElseGet(() -> saveMessage(
            principal,
            inquiry,
            request,
            liveMessage
        ));
    }

    @Transactional
    public SupportMessageResponse addMessageWithAttachments(
        MemberPrincipal principal,
        Long inquiryId,
        UUID clientMessageId,
        String content,
        List<MultipartFile> files
    ) {
        List<MultipartFile> safeFiles = files == null ? List.of() : files;
        if (safeFiles.isEmpty() || safeFiles.size() > SupportAttachmentStorageService.MAX_FILES_PER_MESSAGE) {
            throw new BusinessException(
                safeFiles.isEmpty()
                    ? ErrorCode.FILE_EMPTY
                    : ErrorCode.SUPPORT_ATTACHMENT_COUNT_EXCEEDED
            );
        }
        if (safeFiles.stream().mapToLong(MultipartFile::getSize).sum()
            > SupportAttachmentStorageService.MAX_TOTAL_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED);
        }
        attachmentStorageService.validateAll(safeFiles);
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.length() > 2000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        SupportInquiry inquiry = getAccessibleInquiryForUpdate(principal, inquiryId);
        validateWritable(inquiry);
        boolean liveMessage = inquiry.getStatus() == SupportInquiryStatus.LIVE_ACTIVE;
        if (!liveMessage && (
            inquiry.getStatus() == SupportInquiryStatus.LIVE_REQUESTED
                || inquiry.getStatus() == SupportInquiryStatus.LIVE_OFFERED
        )) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_STATUS_INVALID);
        }
        return messageRepository.findByInquiryIdAndClientMessageId(inquiryId, clientMessageId)
            .map(SupportMessageResponse::from)
            .orElseGet(() -> saveMessageWithAttachments(
                principal,
                inquiry,
                clientMessageId,
                normalizedContent,
                safeFiles,
                liveMessage
            ));
    }

    public SupportAttachment getAttachment(MemberPrincipal principal, Long attachmentId) {
        SupportAttachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND));
        SupportInquiry inquiry = attachment.getMessage().getInquiry();
        if (principal.role() != MemberRole.ROLE_ADMIN
            && !inquiry.getMember().getId().equals(principal.memberId())) {
            throw new BusinessException(ErrorCode.SUPPORT_ATTACHMENT_NOT_FOUND);
        }
        return attachment;
    }

    public Resource loadAttachment(SupportAttachment attachment) {
        return attachmentStorageService.load(attachment.getStoredPath());
    }

    @Transactional
    public SupportInquiryDetailResponse requestLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = getOwnedInquiry(principal.memberId(), inquiryId);
        validateCanStartLive(inquiry);
        SupportChatSession session = renewSession(
            inquiry,
            null,
            SupportChatInitiator.USER_REQUEST
        );
        inquiry.markLiveRequested();
        addSystemMessage(inquiry, getMember(principal.memberId()), "실시간 상담을 요청했습니다.");
        notifyAdmins(
            inquiry,
            NotificationType.SUPPORT_LIVE_REQUESTED,
            "실시간 상담 요청이 도착했습니다",
            "고객센터 문의 #%d의 상담 요청을 확인해 주세요.".formatted(inquiryId)
        );
        publishChanged(inquiry, "SUPPORT_LIVE_REQUESTED");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse offerLive(MemberPrincipal principal, Long inquiryId) {
        requireAdmin(principal);
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
        validateCanStartLive(inquiry);
        Member admin = getMember(principal.memberId());
        inquiry.assign(admin);
        SupportChatSession session = renewSession(
            inquiry,
            admin,
            SupportChatInitiator.ADMIN_OFFER
        );
        inquiry.markLiveOffered();
        addSystemMessage(inquiry, admin, "관리자가 실시간 상담을 제안했습니다.");
        notifyRequester(
            inquiry,
            NotificationType.SUPPORT_LIVE_OFFERED,
            "실시간 상담 제안이 도착했습니다",
            "문의 #%d에서 관리자가 실시간 상담을 제안했습니다.".formatted(inquiryId)
        );
        publishChanged(inquiry, "SUPPORT_LIVE_OFFERED");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse acceptLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
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
            ? getMember(principal.memberId())
            : session.getAdmin();
        inquiry.assign(admin);
        inquiry.markLiveActive();
        session.accept(admin);
        addSystemMessage(inquiry, getMember(principal.memberId()), "실시간 상담이 시작되었습니다.");
        notifyRequester(
            inquiry,
            NotificationType.SUPPORT_LIVE_STATUS,
            "실시간 상담이 시작되었습니다",
            "문의 #%d의 상담 화면을 확인해 주세요.".formatted(inquiryId)
        );
        publishChanged(inquiry, "SUPPORT_LIVE_ACTIVE");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse rejectLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
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
        addSystemMessage(inquiry, getMember(principal.memberId()), "실시간 상담 요청이 거절되었습니다.");
        publishChanged(inquiry, "SUPPORT_LIVE_REJECTED");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse cancelLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
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
        addSystemMessage(inquiry, getMember(principal.memberId()), "실시간 상담 요청이 취소되었습니다.");
        publishChanged(inquiry, "SUPPORT_LIVE_CANCELED");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public int expireWaitingSessions() {
        List<SupportChatSession> sessions = chatSessionRepository.findByStatusAndExpiresAtBefore(
            SupportChatStatus.WAITING,
            LocalDateTime.now()
        );
        sessions.forEach(session -> {
            SupportInquiry inquiry = session.getInquiry();
            session.expire();
            inquiry.resumeGeneralInquiry();
            addSystemMessage(inquiry, inquiry.getMember(), "실시간 상담 요청이 만료되었습니다.");
            publishChanged(inquiry, "SUPPORT_LIVE_EXPIRED");
            realtimePublisher.publishInquiry(
                inquiry.getId(),
                SupportChatSessionResponse.from(session)
            );
        });
        return sessions.size();
    }

    @Transactional
    public SupportInquiryDetailResponse endLive(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
        SupportChatSession session = chatSessionRepository.findByInquiryId(inquiryId)
            .filter(candidate -> candidate.getStatus() == SupportChatStatus.ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID));
        session.end();
        inquiry.resumeGeneralInquiry();
        addSystemMessage(inquiry, getMember(principal.memberId()), "실시간 상담이 종료되었습니다.");
        publishChanged(inquiry, "SUPPORT_LIVE_ENDED");
        realtimePublisher.publishInquiry(inquiryId, SupportChatSessionResponse.from(session));
        return toDetail(inquiry);
    }

    @Transactional
    public SupportInquiryDetailResponse close(
        MemberPrincipal principal,
        Long inquiryId,
        SupportResolutionRequest request
    ) {
        requireAdmin(principal);
        SupportInquiry inquiry = getAccessibleInquiry(principal, inquiryId);
        validateWritable(inquiry);
        Member admin = getMember(principal.memberId());
        inquiry.assign(admin);
        chatSessionRepository.findByInquiryId(inquiryId)
            .filter(session -> session.getStatus() == SupportChatStatus.ACTIVE)
            .ifPresent(SupportChatSession::end);
        messageRepository.save(new SupportMessage(
            inquiry,
            admin,
            SupportMessageType.RESOLUTION,
            request.content().trim(),
            UUID.randomUUID()
        ));
        inquiry.close();
        notifyRequester(
            inquiry,
            NotificationType.SUPPORT_CLOSED,
            "고객센터 문의가 처리 완료되었습니다",
            "문의 #%d가 종료되었습니다. 추가 문의는 새 문의로 등록해 주세요.".formatted(inquiryId)
        );
        publishChanged(inquiry, "SUPPORT_CLOSED");
        return toDetail(inquiry);
    }

    public void validateSubscription(MemberPrincipal principal, Long inquiryId) {
        getAccessibleInquiry(principal, inquiryId);
    }

    private SupportMessageResponse saveMessage(
        MemberPrincipal principal,
        SupportInquiry inquiry,
        SupportMessageCreateRequest request,
        boolean liveMessage
    ) {
        Member author = getMember(principal.memberId());
        boolean admin = principal.role() == MemberRole.ROLE_ADMIN;
        SupportMessage message = messageRepository.save(new SupportMessage(
            inquiry,
            author,
            liveMessage
                ? SupportMessageType.LIVE_CHAT
                : admin ? SupportMessageType.REPLY : SupportMessageType.INQUIRY,
            request.content().trim(),
            request.clientMessageId()
        ));
        if (!liveMessage) {
            if (admin) {
                inquiry.assign(author);
                inquiry.markAnswered();
                notifyRequester(
                    inquiry,
                    NotificationType.SUPPORT_REPLY,
                    "고객센터 답변이 등록되었습니다",
                    "문의 #%d에 관리자 답변이 등록되었습니다.".formatted(inquiry.getId())
                );
            } else {
                inquiry.markWaiting();
                notifyAdmins(
                    inquiry,
                    NotificationType.SUPPORT_INQUIRY_CREATED,
                    "고객센터 추가 문의가 등록되었습니다",
                    "문의 #%d의 추가 내용을 확인해 주세요.".formatted(inquiry.getId())
                );
            }
        }
        SupportMessageResponse response = SupportMessageResponse.from(message);
        realtimePublisher.publishInquiry(inquiry.getId(), response);
        publishChanged(inquiry, liveMessage ? "SUPPORT_LIVE_MESSAGE" : "SUPPORT_MESSAGE_CREATED");
        return response;
    }

    private SupportMessageResponse saveMessageWithAttachments(
        MemberPrincipal principal,
        SupportInquiry inquiry,
        UUID clientMessageId,
        String content,
        List<MultipartFile> files,
        boolean liveMessage
    ) {
        Member author = getMember(principal.memberId());
        boolean admin = principal.role() == MemberRole.ROLE_ADMIN;
        SupportMessage message = messageRepository.save(new SupportMessage(
            inquiry,
            author,
            liveMessage
                ? SupportMessageType.LIVE_CHAT
                : admin ? SupportMessageType.REPLY : SupportMessageType.INQUIRY,
            content,
            clientMessageId
        ));
        files.stream()
            .map(attachmentStorageService::store)
            .map(stored -> attachmentRepository.save(new SupportAttachment(
                message,
                stored.originalFileName(),
                stored.storedPath(),
                stored.contentType(),
                stored.fileSize()
            )))
            .forEach(message.getAttachments()::add);
        if (!liveMessage) {
            if (admin) {
                inquiry.assign(author);
                inquiry.markAnswered();
                notifyRequester(
                    inquiry,
                    NotificationType.SUPPORT_REPLY,
                    "고객센터 답변이 등록되었습니다",
                    "문의 #%d의 관리자 답변이 등록되었습니다.".formatted(inquiry.getId())
                );
            } else {
                inquiry.markWaiting();
                notifyAdmins(
                    inquiry,
                    NotificationType.SUPPORT_INQUIRY_CREATED,
                    "고객센터 추가 문의가 등록되었습니다",
                    "문의 #%d의 추가 내용을 확인해 주세요.".formatted(inquiry.getId())
                );
            }
        }
        SupportMessageResponse response = SupportMessageResponse.from(message);
        realtimePublisher.publishInquiry(inquiry.getId(), response);
        publishChanged(inquiry, liveMessage ? "SUPPORT_LIVE_MESSAGE" : "SUPPORT_MESSAGE_CREATED");
        return response;
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

    private SupportInquiry getAccessibleInquiry(MemberPrincipal principal, Long inquiryId) {
        if (principal.role() == MemberRole.ROLE_ADMIN) {
            return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
        }
        return getOwnedInquiry(principal.memberId(), inquiryId);
    }

    private SupportInquiry getAccessibleInquiryForUpdate(
        MemberPrincipal principal,
        Long inquiryId
    ) {
        SupportInquiry inquiry = inquiryRepository.findByIdForUpdate(inquiryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
        if (principal.role() != MemberRole.ROLE_ADMIN
            && !inquiry.getMember().getId().equals(principal.memberId())) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND);
        }
        return inquiry;
    }

    private SupportInquiry getOwnedInquiry(Long memberId, Long inquiryId) {
        return inquiryRepository.findByIdAndMemberId(inquiryId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void requireAdmin(MemberPrincipal principal) {
        if (principal.role() != MemberRole.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void validateWritable(SupportInquiry inquiry) {
        if (inquiry.getStatus() == SupportInquiryStatus.CLOSED) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_CLOSED);
        }
    }

    private void validateCanStartLive(SupportInquiry inquiry) {
        validateWritable(inquiry);
        if (inquiry.getStatus() == SupportInquiryStatus.LIVE_ACTIVE
            || inquiry.getStatus() == SupportInquiryStatus.LIVE_REQUESTED
            || inquiry.getStatus() == SupportInquiryStatus.LIVE_OFFERED) {
            throw new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID);
        }
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

    private SupportInquiryDetailResponse toDetail(SupportInquiry inquiry) {
        return toDetail(inquiry, true);
    }

    private SupportInquiryDetailResponse toDetail(
        SupportInquiry inquiry,
        boolean includeResolution
    ) {
        return new SupportInquiryDetailResponse(
            SupportInquirySummaryResponse.from(inquiry),
            inquiry.getRelatedOrderId(),
            inquiry.getRelatedProductId(),
            inquiry.getRelatedSettlementId(),
            chatSessionRepository.findByInquiryId(inquiry.getId())
                .map(SupportChatSessionResponse::from)
                .orElse(null),
            messageRepository.findByInquiryIdOrderByCreatedAtAscIdAsc(inquiry.getId())
                .stream()
                .filter(message -> includeResolution
                    || message.getType() != SupportMessageType.RESOLUTION)
                .map(SupportMessageResponse::from)
                .toList()
        );
    }

    private void notifyAdmins(
        SupportInquiry inquiry,
        NotificationType type,
        String title,
        String message
    ) {
        memberRepository.findAllByRole(MemberRole.ROLE_ADMIN).forEach(admin ->
            notificationService.create(new NotificationEvent(
                UUID.randomUUID(),
                admin.getId(),
                type,
                title,
                message,
                "/admin/support/%d".formatted(inquiry.getId())
            ))
        );
    }

    private void notifyRequester(
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

    private void publishChanged(SupportInquiry inquiry, String type) {
        RealtimeEvent event = RealtimeEvent.of(type, "supportInquiry", inquiry.getId());
        realtimePublisher.publishToAdmins(event);
        realtimePublisher.publishToMember(inquiry.getMember().getId(), event);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
    }
}
