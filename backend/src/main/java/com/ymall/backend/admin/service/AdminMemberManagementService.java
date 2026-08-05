package com.ymall.backend.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.dto.AdminAuditLogResponse;
import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminMemberRestrictionRequest;
import com.ymall.backend.admin.dto.AdminSessionRevokeRequest;
import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminAuditLog;
import com.ymall.backend.admin.entity.AdminAuditTargetType;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.admin.repository.AdminAuditLogRepository;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAccessStatus;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AdminMemberManagementService {

    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefreshTokenService refreshTokenService;
    private final AdminAuditService auditService;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminAccessService accessService;
    private final AdminPageRequestFactory pageRequestFactory;

    PageResponse<AdminMemberResponse> getMembers(
        Long actorMemberId,
        int page,
        int size,
        String keyword,
        MemberAccessStatus accessStatus,
        MemberRole role,
        AdminGrade adminGrade,
        LocalDate joinedFrom,
        LocalDate joinedTo
    ) {
        Member actor = accessService.requireAdmin(actorMemberId);
        Page<Member> members = memberRepository.findAll(
            memberSpecification(
                normalize(keyword),
                accessStatus,
                role,
                adminGrade,
                joinedFrom == null ? null : joinedFrom.atStartOfDay(),
                joinedTo == null ? null : joinedTo.plusDays(1).atStartOfDay()
            ),
            pageRequestFactory.create(page, size)
        );
        return toMemberPage(members, accessService.canReadSensitiveMemberData(actor));
    }

    AdminMemberResponse getMember(Long actorMemberId, Long memberId) {
        Member actor = accessService.requireAdmin(actorMemberId);
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return toMemberResponseWithMetrics(
            member,
            accessService.canReadSensitiveMemberData(actor)
        );
    }

    @Transactional
    AdminMemberResponse changeMemberRestriction(
        Long actorMemberId,
        Long memberId,
        AdminMemberRestrictionRequest request
    ) {
        Member actor = memberRepository.findByIdForUpdate(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member target = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        validateMemberOperation(actor, target);
        MemberAccessStatus beforeStatus = target.getAccessStatus();
        MemberAccessStatus afterStatus = request.restricted()
            ? MemberAccessStatus.RESTRICTED
            : MemberAccessStatus.ACTIVE;
        if (beforeStatus == afterStatus) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_INVALID);
        }
        String reason = request.reason().trim();
        if (request.restricted()) target.restrict(actor, reason);
        else target.restoreAccess();
        auditService.record(
            actor,
            AdminAuditTargetType.MEMBER,
            target.getId(),
            AdminAuditAction.MEMBER_RESTRICTION_CHANGED,
            beforeStatus.name(),
            afterStatus.name(),
            reason
        );
        refreshTokenService.revokeAll(target.getId());
        return toMemberResponseWithMetrics(
            target,
            accessService.canReadSensitiveMemberData(actor)
        );
    }

    @Transactional
    void revokeMemberSessions(
        Long actorMemberId,
        Long memberId,
        AdminSessionRevokeRequest request
    ) {
        Member actor = memberRepository.findByIdForUpdate(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member target = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        validateMemberOperation(actor, target);
        target.revokeSessions();
        auditService.record(
            actor,
            AdminAuditTargetType.MEMBER,
            target.getId(),
            AdminAuditAction.MEMBER_SESSIONS_REVOKED,
            null,
            null,
            request.reason().trim()
        );
        refreshTokenService.revokeAll(target.getId());
    }

    List<AdminAuditLogResponse> getMemberAuditLogs(Long actorMemberId, Long memberId) {
        Member actor = accessService.requireAdmin(actorMemberId);
        List<AdminAuditLog> logs = actor.getAdminGrade().hasPermission(AdminPermission.AUDIT_ALL_READ)
            ? auditLogRepository.findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
                AdminAuditTargetType.MEMBER,
                memberId
            )
            : auditLogRepository.findTop20ByActorIdAndTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
                actorMemberId,
                AdminAuditTargetType.MEMBER,
                memberId
            );
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    private Specification<Member> memberSpecification(
        String keyword,
        MemberAccessStatus accessStatus,
        MemberRole role,
        AdminGrade adminGrade,
        LocalDateTime createdFrom,
        LocalDateTime createdTo
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!keyword.isBlank()) {
                String keywordPattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("name")), keywordPattern),
                    builder.like(builder.lower(root.get("email")), keywordPattern)
                ));
            }
            if (accessStatus != null) {
                predicates.add(builder.equal(root.get("accessStatus"), accessStatus));
            }
            if (role != null) {
                predicates.add(builder.equal(root.get("role"), role));
            }
            if (adminGrade != null) {
                predicates.add(builder.equal(root.get("adminGrade"), adminGrade));
            }
            if (createdFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), createdTo));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private PageResponse<AdminMemberResponse> toMemberPage(
        Page<Member> members,
        boolean revealSensitiveData
    ) {
        List<Long> memberIds = members.stream().map(Member::getId).toList();
        Map<Long, Long> orderCounts = new HashMap<>();
        Map<Long, BigDecimal> paidAmounts = new HashMap<>();
        if (!memberIds.isEmpty()) {
            orderRepository.countOrdersByMemberIds(memberIds).forEach(row ->
                orderCounts.put((Long) row[0], (Long) row[1]));
            paymentRepository.sumApprovedAmountByMemberIds(memberIds, PaymentResult.SUCCESS)
                .forEach(row -> paidAmounts.put((Long) row[0], (BigDecimal) row[1]));
        }
        return PageResponse.from(members.map(member -> toMemberResponse(
            member,
            revealSensitiveData,
            orderCounts.getOrDefault(member.getId(), 0L),
            paidAmounts.getOrDefault(member.getId(), BigDecimal.ZERO)
        )));
    }

    private AdminMemberResponse toMemberResponseWithMetrics(
        Member member,
        boolean revealSensitiveData
    ) {
        Long memberId = member.getId();
        long orderCount = orderRepository.countOrdersByMemberIds(List.of(memberId)).stream()
            .findFirst()
            .map(row -> (Long) row[1])
            .orElse(0L);
        BigDecimal totalPaidAmount = paymentRepository
            .sumApprovedAmountByMemberIds(List.of(memberId), PaymentResult.SUCCESS).stream()
            .findFirst()
            .map(row -> (BigDecimal) row[1])
            .orElse(BigDecimal.ZERO);
        return toMemberResponse(member, revealSensitiveData, orderCount, totalPaidAmount);
    }

    private AdminMemberResponse toMemberResponse(
        Member member,
        boolean revealSensitiveData,
        long orderCount,
        BigDecimal totalPaidAmount
    ) {
        return new AdminMemberResponse(
            member.getId(),
            revealSensitiveData ? member.getEmail() : accessService.maskEmail(member.getEmail()),
            member.getName(),
            member.getRole(),
            member.getAdminGrade(),
            member.getAccessStatus(),
            member.getLastLoginAt(),
            member.getRestrictionReason(),
            member.getRestrictedAt(),
            member.getRestrictedBy() == null ? null : member.getRestrictedBy().getId(),
            orderCount,
            totalPaidAmount,
            member.getCreatedAt()
        );
    }

    private AdminAuditLogResponse toAuditLogResponse(AdminAuditLog log) {
        return new AdminAuditLogResponse(
            log.getId(),
            log.getActor().getId(),
            log.getActor().getName(),
            log.getActorGrade(),
            log.getAction(),
            log.getBeforeValue(),
            log.getAfterValue(),
            log.getReason(),
            log.getCreatedAt()
        );
    }

    private void validateMemberOperation(Member actor, Member target) {
        if (actor.getRole() != MemberRole.ROLE_ADMIN || actor.getAdminGrade() == null
            || actor.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        AdminGrade actorGrade = actor.getAdminGrade();
        if (!actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_LIMITED)
            && !actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_ALL)) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        if (!actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_ALL)
            && target.getRole() != MemberRole.ROLE_USER) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        if (target.getRole() == MemberRole.ROLE_ADMIN) {
            if (target.getAdminGrade() == null
                || target.getAdminGrade().level() >= actorGrade.level()) {
                throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
            }
        }
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
