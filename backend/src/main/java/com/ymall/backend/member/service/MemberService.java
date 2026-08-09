package com.ymall.backend.member.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.AuthenticationTokens;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.dto.EmailAvailabilityResponse;
import com.ymall.backend.member.dto.MemberLoginRequest;
import com.ymall.backend.member.dto.MemberPasswordChangeRequest;
import com.ymall.backend.member.dto.MemberProfileResponse;
import com.ymall.backend.member.dto.MemberProfileUpdateRequest;
import com.ymall.backend.member.dto.MemberResponse;
import com.ymall.backend.member.dto.MemberSignupRequest;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.util.EmailAddressNormalizer;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final SignupEmailVerificationService signupEmailVerificationService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    public EmailAvailabilityResponse checkEmailAvailability(String requestedEmail) {
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        return new EmailAvailabilityResponse(!memberRepository.existsByEmailIgnoreCase(email));
    }

    public MemberProfileResponse getProfile(Long memberId) {
        return toProfileResponse(findMember(memberId));
    }

    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, MemberProfileUpdateRequest request) {
        Member member = findMember(memberId);
        member.updateProfile(request.name(), request.phone());
        return toProfileResponse(member);
    }

    @Transactional
    public void changePassword(Long memberId, MemberPasswordChangeRequest request) {
        Member member = findMember(memberId);
        if (!member.hasPassword()
            || !passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public MemberResponse signup(MemberSignupRequest request) {
        String email = EmailAddressNormalizer.normalize(request.email());
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        signupEmailVerificationService.consume(request.emailVerificationToken(), email);

        Member member = new Member(
            email,
            passwordEncoder.encode(request.password()),
            request.name().trim(),
            request.phone(),
            MemberRole.ROLE_USER
        );
        Member savedMember;
        try {
            savedMember = memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        return new MemberResponse(
            savedMember.getId(),
            savedMember.getEmail(),
            savedMember.getName(),
            savedMember.getPhone(),
            savedMember.getRole(),
            savedMember.getCreatedAt()
        );
    }

    @Transactional
    public AuthenticationTokens login(MemberLoginRequest request) {
        String email = EmailAddressNormalizer.normalize(request.email());
        loginAttemptLimiter.consume(email);
        Member member = memberRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        if (!member.hasPassword() || !passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        AuthenticationTokens tokens = refreshTokenService.issueForLogin(member);
        loginAttemptLimiter.reset(email);
        return tokens;
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private MemberProfileResponse toProfileResponse(Member member) {
        return new MemberProfileResponse(
            member.getId(),
            member.getEmail(),
            member.getName(),
            member.getPhone(),
            member.hasPassword(),
            member.getRole(),
            member.getCreatedAt()
        );
    }
}
