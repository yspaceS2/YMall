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
import com.ymall.backend.support.entity.SupportRequesterType;
import com.ymall.backend.support.repository.SupportChatSessionRepository;
import com.ymall.backend.support.repository.SupportInquiryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportService {

    private static final int MAX_PAGE_SIZE = 100;
    private final SupportInquiryRepository inquiryRepository;
    private final SupportMessageService messageService;
    private final SupportAttachmentService attachmentService;
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
        messageService.createInitialMessage(inquiry, member, request.content());
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
        return messageService.addMessage(principal, inquiryId, request, liveMessage);
    }

    @Transactional
    public SupportMessageResponse addMessageWithAttachments(
        MemberPrincipal principal,
        Long inquiryId,
        UUID clientMessageId,
        String content,
        List<MultipartFile> files
    ) {
        return messageService.addMessageWithAttachments(
            principal,
            inquiryId,
            clientMessageId,
            content,
            files
        );
    }

    public SupportAttachment getAttachment(MemberPrincipal principal, Long attachmentId) {
        return attachmentService.getAccessibleAttachment(principal, attachmentId);
    }

    public Resource loadAttachment(SupportAttachment attachment) {
        return attachmentService.load(attachment);
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
        messageService.createResolutionMessage(inquiry, admin, request.content());
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
            messageService.getMessages(inquiry.getId(), includeResolution)
        );
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );
    }
}
