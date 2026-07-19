package com.ymall.backend.member.service;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.OAuthFlowContext;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class OAuthEmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final OAuthFlowContext oAuthFlowContext;

    @Value("${ymall.mail.from}")
    private String from;

    public void send(HttpServletRequest request, String requestedEmail) {
        oAuthFlowContext.get(request)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
        String email = normalize(requestedEmail);
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[YMall] 이메일 인증번호");
        message.setText("YMall 소셜 회원가입 인증번호는 " + code + "입니다. 5분 안에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_DELIVERY_FAILED);
        }
        oAuthFlowContext.startEmailVerification(request, email, code);
    }

    public void confirm(HttpServletRequest request, String requestedEmail, String code) {
        String email = normalize(requestedEmail);
        if (!oAuthFlowContext.verifyEmail(request, email, code)) {
            throw new BusinessException(ErrorCode.OAUTH_EMAIL_VERIFICATION_FAILED);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
