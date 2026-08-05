package com.ymall.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
class AdminAccessService {

    private final MemberRepository memberRepository;

    Member requireAdmin(Long actorMemberId) {
        Member actor = memberRepository.findById(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (actor.getRole() != MemberRole.ROLE_ADMIN || actor.getAdminGrade() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return actor;
    }

    boolean canReadSensitiveMemberData(Member actor) {
        return actor.getAdminGrade() == AdminGrade.SUPER_ADMIN;
    }

    String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 1) return "***" + email.substring(Math.max(separator, 0));
        return email.substring(0, 1) + "***" + email.substring(separator);
    }

    String maskBusinessNumber(String businessNumber) {
        if (businessNumber.length() <= 5) return "***";
        return businessNumber.substring(0, 3)
            + "-**-"
            + businessNumber.substring(businessNumber.length() - 4);
    }
}
