package com.ymall.backend.member.service;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.global.util.SecurityTokenUtils;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.util.EmailAddressNormalizer;

@Service
@RequiredArgsConstructor
public class OAuthEmailVerificationService {

    private final MemberMailSender memberMailSender;
    private final MemberRepository memberRepository;
    private final OAuthFlowContext oAuthFlowContext;

    public void send(HttpServletRequest request, String requestedEmail) {
        oAuthFlowContext.get(request)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        String code = SecurityTokenUtils.generateSixDigitCode();
        try {
            memberMailSender.send(
                email,
                "[YMall] 이메일 인증번호",
                "YMall 소셜 회원가입 인증번호는 " + code + "입니다. 5분 안에 입력해 주세요."
            );
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_DELIVERY_FAILED);
        }
        oAuthFlowContext.startEmailVerification(request, email, code);
    }

    public void confirm(HttpServletRequest request, String requestedEmail, String code) {
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        if (!oAuthFlowContext.verifyEmail(request, email, code)) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_VERIFICATION_FAILED);
        }
    }
}
