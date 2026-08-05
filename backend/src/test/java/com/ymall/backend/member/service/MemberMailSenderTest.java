package com.ymall.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class MemberMailSenderTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void sendsMessageWithConfiguredSender() {
        MemberMailSender memberMailSender = new MemberMailSender(
            javaMailSender,
            "no-reply@ymall.local"
        );

        memberMailSender.send("member@example.com", "subject", "body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(
            SimpleMailMessage.class
        );
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("no-reply@ymall.local");
        assertThat(message.getTo()).containsExactly("member@example.com");
        assertThat(message.getSubject()).isEqualTo("subject");
        assertThat(message.getText()).isEqualTo("body");
    }
}
