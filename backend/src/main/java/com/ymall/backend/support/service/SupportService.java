package com.ymall.backend.support.service;

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
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.support.dto.SupportChatSessionResponse;
import com.ymall.backend.support.dto.SupportInquiryCreateRequest;
import com.ymall.backend.support.dto.SupportInquiryDetailResponse;
import com.ymall.backend.support.dto.SupportInquirySummaryResponse;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.dto.SupportMessageResponse;
import com.ymall.backend.support.dto.SupportPendingCountResponse;
import com.ymall.backend.support.dto.SupportResolutionRequest;
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
    private final SupportInquiryRepository inquiryRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportAttachmentRepository attachmentRepository;
    private final SupportAttachmentStorageService attachmentStorageService;
    private final SupportChatSessionRepository chatSessionRepository;
    private final SupportInquiryAccessService inquiryAccessService;
    private final SupportEventService eventService;
    private final SupportLiveChatService liveChatService;

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
            inquiryAccessService.getAccessibleInquiry(principal, inquiryId),
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
        Member member = inquiryAccessService.getMember(principal.memberId());
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
        eventService.notifyAdmins(
            inquiry,
            NotificationType.SUPPORT_INQUIRY_CREATED,
            "새 고객센터 문의가 등록되었습니다",
            "%s님의 문의를 확인해 주세요.".formatted(member.getName())
        );
        eventService.publishChanged(inquiry, "SUPPORT_INQUIRY_CREATED");
        return toDetail(inquiry);
    }

    @Transactional
    public SupportMessageResponse addMessage(
        MemberPrincipal principal,
        Long inquiryId,
        SupportMessageCreateRequest request,
        boolean liveMessage
    ) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiryForUpdate(principal, inquiryId);
        inquiryAccessService.validateWritable(inquiry);
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
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiryForUpdate(principal, inquiryId);
        inquiryAccessService.validateWritable(inquiry);
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
        return toDetail(liveChatService.requestLive(principal, inquiryId));
    }

    @Transactional
    public SupportInquiryDetailResponse offerLive(MemberPrincipal principal, Long inquiryId) {
        return toDetail(liveChatService.offerLive(principal, inquiryId));
    }

    @Transactional
    public SupportInquiryDetailResponse acceptLive(MemberPrincipal principal, Long inquiryId) {
        return toDetail(liveChatService.acceptLive(principal, inquiryId));
    }

    @Transactional
    public SupportInquiryDetailResponse rejectLive(MemberPrincipal principal, Long inquiryId) {
        return toDetail(liveChatService.rejectLive(principal, inquiryId));
    }

    @Transactional
    public SupportInquiryDetailResponse cancelLive(MemberPrincipal principal, Long inquiryId) {
        return toDetail(liveChatService.cancelLive(principal, inquiryId));
    }

    @Transactional
    public int expireWaitingSessions() {
        return liveChatService.expireWaitingSessions();
    }

    @Transactional
    public SupportInquiryDetailResponse endLive(MemberPrincipal principal, Long inquiryId) {
        return toDetail(liveChatService.endLive(principal, inquiryId));
    }

    @Transactional
    public SupportInquiryDetailResponse close(
        MemberPrincipal principal,
        Long inquiryId,
        SupportResolutionRequest request
    ) {
        inquiryAccessService.requireAdmin(principal);
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
        inquiryAccessService.validateWritable(inquiry);
        Member admin = inquiryAccessService.getMember(principal.memberId());
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
        eventService.notifyRequester(
            inquiry,
            NotificationType.SUPPORT_CLOSED,
            "고객센터 문의가 처리 완료되었습니다",
            "문의 #%d가 종료되었습니다. 추가 문의는 새 문의로 등록해 주세요.".formatted(inquiryId)
        );
        eventService.publishChanged(inquiry, "SUPPORT_CLOSED");
        return toDetail(inquiry);
    }

    public void validateSubscription(MemberPrincipal principal, Long inquiryId) {
        inquiryAccessService.getAccessibleInquiry(principal, inquiryId);
    }

    private SupportMessageResponse saveMessage(
        MemberPrincipal principal,
        SupportInquiry inquiry,
        SupportMessageCreateRequest request,
        boolean liveMessage
    ) {
        Member author = inquiryAccessService.getMember(principal.memberId());
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
                eventService.notifyRequester(
                    inquiry,
                    NotificationType.SUPPORT_REPLY,
                    "고객센터 답변이 등록되었습니다",
                    "문의 #%d에 관리자 답변이 등록되었습니다.".formatted(inquiry.getId())
                );
            } else {
                inquiry.markWaiting();
                eventService.notifyAdmins(
                    inquiry,
                    NotificationType.SUPPORT_INQUIRY_CREATED,
                    "고객센터 추가 문의가 등록되었습니다",
                    "문의 #%d의 추가 내용을 확인해 주세요.".formatted(inquiry.getId())
                );
            }
        }
        SupportMessageResponse response = SupportMessageResponse.from(message);
        eventService.publishInquiry(inquiry.getId());
        eventService.publishChanged(
            inquiry,
            liveMessage ? "SUPPORT_LIVE_MESSAGE" : "SUPPORT_MESSAGE_CREATED"
        );
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
        Member author = inquiryAccessService.getMember(principal.memberId());
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
                eventService.notifyRequester(
                    inquiry,
                    NotificationType.SUPPORT_REPLY,
                    "고객센터 답변이 등록되었습니다",
                    "문의 #%d의 관리자 답변이 등록되었습니다.".formatted(inquiry.getId())
                );
            } else {
                inquiry.markWaiting();
                eventService.notifyAdmins(
                    inquiry,
                    NotificationType.SUPPORT_INQUIRY_CREATED,
                    "고객센터 추가 문의가 등록되었습니다",
                    "문의 #%d의 추가 내용을 확인해 주세요.".formatted(inquiry.getId())
                );
            }
        }
        SupportMessageResponse response = SupportMessageResponse.from(message);
        eventService.publishInquiry(inquiry.getId());
        eventService.publishChanged(
            inquiry,
            liveMessage ? "SUPPORT_LIVE_MESSAGE" : "SUPPORT_MESSAGE_CREATED"
        );
        return response;
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

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
    }
}
