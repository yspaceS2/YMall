package com.ymall.backend.global.security;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAccessStatus;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberPrincipalResolver {

    private final ObjectProvider<MemberRepository> memberRepositoryProvider;

    @Transactional(readOnly = true)
    public MemberPrincipal resolve(MemberPrincipal tokenPrincipal) {
        Member member = memberRepositoryProvider.getObject().findById(tokenPrincipal.memberId())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (member.getAuthVersion() != tokenPrincipal.authVersion()
            || member.getAccessStatus() != MemberAccessStatus.ACTIVE
            || member.getRole() != tokenPrincipal.role()
            || !Objects.equals(member.getEmail(), tokenPrincipal.email())
            || (member.getRole() == MemberRole.ROLE_ADMIN && member.getAdminGrade() == null)
            || (member.getRole() != MemberRole.ROLE_ADMIN && member.getAdminGrade() != null)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return MemberPrincipal.from(member);
    }
}
