package com.ymall.backend.seller.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminAuditTargetType;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.admin.service.AdminAuditService;
import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.dto.SellerApplicationCreateRequest;
import com.ymall.backend.seller.dto.SellerApplicationResponse;
import com.ymall.backend.seller.dto.SellerApplicationReviewRequest;
import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.seller.entity.SellerApplicationStatus;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerApplicationRepository;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerApplicationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<SellerApplicationStatus> ACTIVE_STATUSES = List.of(
        SellerApplicationStatus.PENDING,
        SellerApplicationStatus.APPROVED
    );

    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final AdminAuditService adminAuditService;

    public SellerApplicationResponse getMyApplication(Long memberId) {
        return toResponse(sellerApplicationRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_APPLICATION_NOT_FOUND)));
    }

    @Transactional
    public SellerApplicationResponse apply(
        Long memberId,
        SellerApplicationCreateRequest request
    ) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole() != MemberRole.ROLE_USER
            || sellerProfileRepository.existsByMemberId(memberId)) {
            throw new BusinessException(ErrorCode.SELLER_APPLICATION_NOT_ALLOWED);
        }

        SellerApplication application = sellerApplicationRepository.findByMemberId(memberId)
            .orElse(null);
        if (application != null
            && application.getStatus() != SellerApplicationStatus.REJECTED
            && application.getStatus() != SellerApplicationStatus.NEEDS_REVISION) {
            throw new BusinessException(ErrorCode.SELLER_APPLICATION_ALREADY_EXISTS);
        }
        validateBusinessNumber(request.businessNumber(), application);

        if (application == null) {
            application = new SellerApplication(
                member,
                request.storeName(),
                request.businessNumber(),
                request.description()
            );
        } else {
            application.resubmit(
                request.storeName(),
                request.businessNumber(),
                request.description()
            );
        }
        SellerApplication savedApplication = sellerApplicationRepository.save(application);
        dashboardRealtimePublisher.invalidateAdmins(
            "sellerApplication",
            savedApplication.getId()
        );
        return toResponse(savedApplication);
    }

    public PageResponse<SellerApplicationResponse> getApplications(
        SellerApplicationStatus status,
        int page,
        int size,
        String keyword
    ) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
            pageNumber,
            pageSize,
            Sort.by(Sort.Direction.ASC, "createdAt")
        );
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        return PageResponse.from((normalizedKeyword.isEmpty()
            ? sellerApplicationRepository.findByStatus(status, pageable)
            : sellerApplicationRepository.searchByStatus(
                status,
                normalizedKeyword,
                pageable
            )).map(this::toResponse));
    }

    public SellerApplicationResponse getApplication(Long applicationId) {
        return sellerApplicationRepository.findWithMemberById(applicationId)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SELLER_APPLICATION_NOT_FOUND
            ));
    }

    @Transactional
    public SellerApplicationResponse review(
        Long reviewerId,
        Long applicationId,
        SellerApplicationReviewRequest request
    ) {
        if (request.status() != SellerApplicationStatus.APPROVED
            && request.status() != SellerApplicationStatus.REJECTED
            && request.status() != SellerApplicationStatus.NEEDS_REVISION) {
            throw new BusinessException(ErrorCode.SELLER_APPLICATION_STATUS_INVALID);
        }
        SellerApplication application = sellerApplicationRepository.findByIdForUpdate(applicationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_APPLICATION_NOT_FOUND));
        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.SELLER_APPLICATION_STATUS_INVALID);
        }
        Member reviewer = memberRepository.findById(reviewerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (reviewer.getAdminGrade() == null
            || !reviewer.getAdminGrade().hasPermission(AdminPermission.SELLER_APPLICATION_REVIEW)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String reason = request.rejectionReason() == null
            ? ""
            : request.rejectionReason().trim();
        if (request.status() == SellerApplicationStatus.NEEDS_REVISION) {
            if (reason.isBlank()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
            application.requestRevision(reviewer, reason);
            recordReviewAudit(reviewer, application, "PENDING", "NEEDS_REVISION", reason);
            dashboardRealtimePublisher.invalidateSellerAndAdmins(
                application.getMember().getId(),
                "sellerApplication",
                applicationId
            );
            return toResponse(application);
        }
        if (!reviewer.getAdminGrade().hasPermission(AdminPermission.SELLER_APPLICATION_DECIDE)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if (request.status() == SellerApplicationStatus.REJECTED) {
            if (reason.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            application.reject(reviewer, reason);
            recordReviewAudit(reviewer, application, "PENDING", "REJECTED", reason);
            dashboardRealtimePublisher.invalidateSellerAndAdmins(
                application.getMember().getId(),
                "sellerApplication",
                applicationId
            );
            return toResponse(application);
        }

        Member applicant = application.getMember();
        if (applicant.getRole() != MemberRole.ROLE_USER
            || sellerProfileRepository.existsByMemberId(applicant.getId())) {
            throw new BusinessException(ErrorCode.SELLER_APPLICATION_NOT_ALLOWED);
        }
        if (sellerProfileRepository.existsByBusinessNumber(application.getBusinessNumber())) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_DUPLICATED);
        }

        sellerProfileRepository.save(new SellerProfile(
            applicant,
            application.getStoreName(),
            application.getBusinessNumber(),
            application.getDescription()
        ));
        applicant.promoteToSeller();
        application.approve(reviewer);
        recordReviewAudit(reviewer, application, "PENDING", "APPROVED", "판매자 가입 승인");
        refreshTokenService.revokeAll(applicant.getId());
        dashboardRealtimePublisher.invalidateSellerAndAdmins(
            applicant.getId(),
            "sellerApplication",
            applicationId
        );
        return toResponse(application);
    }

    private void recordReviewAudit(
        Member reviewer,
        SellerApplication application,
        String beforeValue,
        String afterValue,
        String reason
    ) {
        adminAuditService.record(
            reviewer,
            AdminAuditTargetType.SELLER_APPLICATION,
            application.getId(),
            AdminAuditAction.SELLER_APPLICATION_REVIEWED,
            beforeValue,
            afterValue,
            reason
        );
    }

    private void validateBusinessNumber(
        String businessNumber,
        SellerApplication existingApplication
    ) {
        if (sellerProfileRepository.existsByBusinessNumber(businessNumber)) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_DUPLICATED);
        }
        boolean duplicateApplication = existingApplication == null
            ? sellerApplicationRepository.existsByBusinessNumberAndStatusIn(
                businessNumber,
                ACTIVE_STATUSES
            )
            : sellerApplicationRepository.existsByBusinessNumberAndStatusInAndIdNot(
                businessNumber,
                ACTIVE_STATUSES,
                existingApplication.getId()
            );
        if (duplicateApplication) {
            throw new BusinessException(ErrorCode.SELLER_BUSINESS_NUMBER_DUPLICATED);
        }
    }

    private SellerApplicationResponse toResponse(SellerApplication application) {
        Member member = application.getMember();
        return new SellerApplicationResponse(
            application.getId(),
            member.getId(),
            member.getName(),
            member.getEmail(),
            application.getStoreName(),
            application.getBusinessNumber(),
            application.getDescription(),
            application.getStatus(),
            application.getRejectionReason(),
            application.getReviewedAt(),
            application.getCreatedAt(),
            application.getUpdatedAt()
        );
    }
}
