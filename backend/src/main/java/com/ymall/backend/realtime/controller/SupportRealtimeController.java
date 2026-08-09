package com.ymall.backend.realtime.controller;

import java.security.Principal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.service.SupportService;

@Controller
@RequiredArgsConstructor
public class SupportRealtimeController {

    private final SupportService supportService;

    @MessageMapping("/support/inquiries/{inquiryId}/messages")
    public void sendMessage(
        @DestinationVariable Long inquiryId,
        @Valid @Payload SupportMessageCreateRequest request,
        Principal socketPrincipal
    ) {
        supportService.addMessage(principal(socketPrincipal), inquiryId, request, true);
    }

    private MemberPrincipal principal(Principal socketPrincipal) {
        if (!(socketPrincipal instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return principal;
    }
}
