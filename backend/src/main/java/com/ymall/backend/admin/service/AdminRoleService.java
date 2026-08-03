package com.ymall.backend.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminRoleResponse;
import com.ymall.backend.admin.dto.AdminRoleUpdateRequest;
import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminAuditTargetType;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;
    private final AdminAuditService auditService;

    @Transactional
    public AdminRoleResponse changeRole(
        Long actorMemberId,
        Long targetMemberId,
        AdminRoleUpdateRequest request
    ) {
        if (actorMemberId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
        }

        Member actor = findMemberForUpdate(actorMemberId);
        Member target = findMemberForUpdate(targetMemberId);
        AdminGrade actorGrade = requireAdminGrade(actor);
        validateActorPermission(actorGrade);
        validateRequest(request);
        validateTarget(actorGrade, target, request);

        String beforeValue = roleValue(target.getRole(), target.getAdminGrade());
        String afterValue = roleValue(request.role(), request.adminGrade());
        if (beforeValue.equals(afterValue)) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_INVALID);
        }

        target.changeAdminRole(request.role(), request.adminGrade());
        String reason = request.reason().trim();
        auditService.record(
            actor,
            AdminAuditTargetType.MEMBER,
            target.getId(),
            AdminAuditAction.ADMIN_ROLE_CHANGED,
            beforeValue,
            afterValue,
            reason
        );
        refreshTokenService.revokeAll(target.getId());
        return AdminRoleResponse.from(target);
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private AdminGrade requireAdminGrade(Member actor) {
        if (actor.getRole() != MemberRole.ROLE_ADMIN || actor.getAdminGrade() == null) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
        }
        return actor.getAdminGrade();
    }

    private void validateActorPermission(AdminGrade actorGrade) {
        if (!actorGrade.hasPermission(AdminPermission.ADMIN_MANAGER_MANAGE)
            && !actorGrade.hasPermission(AdminPermission.ADMIN_ALL_MANAGE)) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
        }
    }

    private void validateRequest(AdminRoleUpdateRequest request) {
        if (request.role() == MemberRole.ROLE_ADMIN) {
            if (request.adminGrade() == null || request.adminGrade() == AdminGrade.SUPER_ADMIN) {
                throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_INVALID);
            }
            return;
        }
        if (request.role() != MemberRole.ROLE_USER || request.adminGrade() != null) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_INVALID);
        }
    }

    private void validateTarget(
        AdminGrade actorGrade,
        Member target,
        AdminRoleUpdateRequest request
    ) {
        if (target.getRole() == MemberRole.ROLE_SELLER) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_INVALID);
        }
        if (target.getRole() == MemberRole.ROLE_ADMIN) {
            AdminGrade targetGrade = target.getAdminGrade();
            if (targetGrade == null || targetGrade.level() >= actorGrade.level()) {
                throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
            }
        }

        boolean allowed = actorGrade == AdminGrade.SUPERVISOR
            ? isSupervisorTransition(target, request)
            : actorGrade == AdminGrade.SUPER_ADMIN && isSuperAdminTransition(target, request);
        if (!allowed) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
        }
    }

    private boolean isSupervisorTransition(Member target, AdminRoleUpdateRequest request) {
        return (target.getRole() == MemberRole.ROLE_USER
            && request.role() == MemberRole.ROLE_ADMIN
            && request.adminGrade() == AdminGrade.MANAGER)
            || (target.getRole() == MemberRole.ROLE_ADMIN
            && target.getAdminGrade() == AdminGrade.MANAGER
            && request.role() == MemberRole.ROLE_USER);
    }

    private boolean isSuperAdminTransition(Member target, AdminRoleUpdateRequest request) {
        if (target.getRole() == MemberRole.ROLE_USER) {
            return request.role() == MemberRole.ROLE_ADMIN
                && (request.adminGrade() == AdminGrade.MANAGER
                || request.adminGrade() == AdminGrade.SUPERVISOR);
        }
        if (target.getAdminGrade() == AdminGrade.MANAGER) {
            return request.role() == MemberRole.ROLE_USER
                || (request.role() == MemberRole.ROLE_ADMIN
                && request.adminGrade() == AdminGrade.SUPERVISOR);
        }
        if (target.getAdminGrade() == AdminGrade.SUPERVISOR) {
            return request.role() == MemberRole.ROLE_USER
                || (request.role() == MemberRole.ROLE_ADMIN
                && request.adminGrade() == AdminGrade.MANAGER);
        }
        return false;
    }

    private String roleValue(MemberRole role, AdminGrade adminGrade) {
        return adminGrade == null ? role.name() : role.name() + ":" + adminGrade.name();
    }
}
