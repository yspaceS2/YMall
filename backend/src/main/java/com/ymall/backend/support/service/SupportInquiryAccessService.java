package com.ymall.backend.support.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.repository.SupportInquiryRepository;

@Service
@RequiredArgsConstructor
class SupportInquiryAccessService {

    private final SupportInquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    SupportInquiry getAccessibleInquiry(MemberPrincipal principal, Long inquiryId) {
        if (principal.role() == MemberRole.ROLE_ADMIN) {
            return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
        }
        return getOwnedInquiry(principal.memberId(), inquiryId);
    }

    SupportInquiry getAccessibleInquiryForUpdate(MemberPrincipal principal, Long inquiryId) {
        SupportInquiry inquiry = inquiryRepository.findByIdForUpdate(inquiryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
        if (principal.role() != MemberRole.ROLE_ADMIN
            && !inquiry.getMember().getId().equals(principal.memberId())) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND);
        }
        return inquiry;
    }

    SupportInquiry getOwnedInquiry(Long memberId, Long inquiryId) {
        return inquiryRepository.findByIdAndMemberId(inquiryId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SUPPORT_INQUIRY_NOT_FOUND));
    }

    Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    void requireAdmin(MemberPrincipal principal) {
        if (principal.role() != MemberRole.ROLE_ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    void validateWritable(SupportInquiry inquiry) {
        if (inquiry.getStatus() == SupportInquiryStatus.CLOSED) {
            throw new BusinessException(ErrorCode.SUPPORT_INQUIRY_CLOSED);
        }
    }

    void validateCanStartLive(SupportInquiry inquiry) {
        validateWritable(inquiry);
        if (inquiry.getStatus() == SupportInquiryStatus.LIVE_ACTIVE
            || inquiry.getStatus() == SupportInquiryStatus.LIVE_REQUESTED
            || inquiry.getStatus() == SupportInquiryStatus.LIVE_OFFERED) {
            throw new BusinessException(ErrorCode.SUPPORT_CHAT_STATUS_INVALID);
        }
    }
}
