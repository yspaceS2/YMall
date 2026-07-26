package com.ymall.backend.member.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberEmailChangedEventListener {

    private static final Logger log = LoggerFactory.getLogger(MemberEmailChangedEventListener.class);

    private final JavaMailSender mailSender;

    @Value("${ymall.mail.from}")
    private String from;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MemberEmailChangedEvent event) {
        log.info("Member email changed: memberId={}", event.memberId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.previousEmail());
        message.setSubject("[YMall] 이메일 변경 안내");
        message.setText(
            "YMall 로그인 이메일이 " + mask(event.newEmail())
                + " 주소로 변경되었습니다. 본인이 변경하지 않았다면 고객센터에 문의해 주세요."
        );
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Email change security notification delivery failed: memberId={}", event.memberId());
        }
    }

    private String mask(String email) {
        int separator = email.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(separator);
    }
}
