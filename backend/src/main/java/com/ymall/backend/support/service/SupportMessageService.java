package com.ymall.backend.support.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.dto.SupportMessageResponse;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.entity.SupportMessage;
import com.ymall.backend.support.entity.SupportMessageType;
import com.ymall.backend.support.repository.SupportMessageRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SupportMessageService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final SupportMessageRepository messageRepository;
    private final SupportInquiryAccessService inquiryAccessService;
    private final SupportAttachmentService attachmentService;
    private final SupportEventService eventService;

    @Transactional
    SupportMessageResponse addMessage(
        MemberPrincipal principal,
        Long inquiryId,
        SupportMessageCreateRequest request,
        boolean liveMessage
    ) {
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiryForUpdate(principal, inquiryId);
        validateMessageStatus(inquiry, liveMessage);
        return messageRepository.findByInquiryIdAndClientMessageId(
            inquiryId,
            request.clientMessageId()
        ).map(SupportMessageResponse::from).orElseGet(() -> saveMessage(
            principal,
            inquiry,
            request.clientMessageId(),
            request.content().trim(),
            List.of(),
            liveMessage
        ));
    }

    @Transactional
    SupportMessageResponse addMessageWithAttachments(
        MemberPrincipal principal,
        Long inquiryId,
        UUID clientMessageId,
        String content,
        List<MultipartFile> files
    ) {
        List<MultipartFile> safeFiles = attachmentService.validateFiles(files);
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        SupportInquiry inquiry = inquiryAccessService.getAccessibleInquiryForUpdate(principal, inquiryId);
        boolean liveMessage = inquiry.getStatus() == SupportInquiryStatus.LIVE_ACTIVE;
        validateMessageStatus(inquiry, liveMessage);
        return messageRepository.findByInquiryIdAndClientMessageId(inquiryId, clientMessageId)
            .map(SupportMessageResponse::from)
            .orElseGet(() -> saveMessage(
                principal,
                inquiry,
                clientMessageId,
                normalizedContent,
                safeFiles,
                liveMessage
            ));
    }

    @Transactional
    void createInitialMessage(SupportInquiry inquiry, Member author, String content) {
        messageRepository.save(new SupportMessage(
            inquiry,
            author,
            SupportMessageType.INQUIRY,
            content.trim(),
            UUID.randomUUID()
        ));
    }

    @Transactional
    void createResolutionMessage(SupportInquiry inquiry, Member admin, String content) {
        messageRepository.save(new SupportMessage(
            inquiry,
            admin,
            SupportMessageType.RESOLUTION,
            content.trim(),
            UUID.randomUUID()
        ));
    }

    List<SupportMessageResponse> getMessages(Long inquiryId, boolean includeResolution) {
        return messageRepository.findByInquiryIdOrderByCreatedAtAscIdAsc(inquiryId)
            .stream()
            .filter(message -> includeResolution
                || message.getType() != SupportMessageType.RESOLUTION)
            .map(SupportMessageResponse::from)
            .toList();
    }

    private void validateMessageStatus(SupportInquiry inquiry, boolean liveMessage) {
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
    }

    private SupportMessageResponse saveMessage(
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
            messageType(admin, liveMessage),
            content,
            clientMessageId
        ));
        attachmentService.storeAll(message, files);
        updateInquiryAfterMessage(inquiry, author, admin, liveMessage, !files.isEmpty());
        SupportMessageResponse response = SupportMessageResponse.from(message);
        eventService.publishInquiry(inquiry.getId());
        eventService.publishChanged(
            inquiry,
            liveMessage ? "SUPPORT_LIVE_MESSAGE" : "SUPPORT_MESSAGE_CREATED"
        );
        return response;
    }

    private SupportMessageType messageType(boolean admin, boolean liveMessage) {
        if (liveMessage) {
            return SupportMessageType.LIVE_CHAT;
        }
        return admin ? SupportMessageType.REPLY : SupportMessageType.INQUIRY;
    }

    private void updateInquiryAfterMessage(
        SupportInquiry inquiry,
        Member author,
        boolean admin,
        boolean liveMessage,
        boolean hasAttachments
    ) {
        if (liveMessage) {
            return;
        }
        if (admin) {
            inquiry.assign(author);
            inquiry.markAnswered();
            eventService.notifyRequester(
                inquiry,
                NotificationType.SUPPORT_REPLY,
                "고객센터 답변이 등록되었습니다",
                (hasAttachments
                    ? "문의 #%d의 관리자 답변이 등록되었습니다."
                    : "문의 #%d에 관리자 답변이 등록되었습니다.").formatted(inquiry.getId())
            );
            return;
        }
        inquiry.markWaiting();
        eventService.notifyAdmins(
            inquiry,
            NotificationType.SUPPORT_INQUIRY_CREATED,
            "고객센터 추가 문의가 등록되었습니다",
            "문의 #%d의 추가 내용을 확인해 주세요.".formatted(inquiry.getId())
        );
    }
}
