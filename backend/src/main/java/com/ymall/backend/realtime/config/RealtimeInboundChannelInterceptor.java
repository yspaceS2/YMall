package com.ymall.backend.realtime.config;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.support.repository.SupportInquiryRepository;

@Component
@RequiredArgsConstructor
public class RealtimeInboundChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INQUIRY_TOPIC = "/topic/support/inquiries/";
    private static final String MEMBER_TOPIC = "/topic/realtime/members/";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectProvider<SupportInquiryRepository> inquiryRepositoryProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message,
            StompHeaderAccessor.class
        );
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        MemberPrincipal principal = jwtTokenProvider.parseAccessToken(
            authorization.substring(BEARER_PREFIX.length())
        );
        accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority(principal.role().name()))
        ));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        MemberPrincipal principal = principal(accessor);
        String destination = accessor.getDestination();
        if (destination == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (destination.startsWith(INQUIRY_TOPIC)) {
            Long inquiryId = parseId(destination, INQUIRY_TOPIC);
            SupportInquiryRepository inquiryRepository = inquiryRepositoryProvider.getObject();
            boolean allowed = principal.role() == MemberRole.ROLE_ADMIN
                ? inquiryRepository.existsById(inquiryId)
                : inquiryRepository.findByIdAndMemberId(inquiryId, principal.memberId()).isPresent();
            if (!allowed) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        if (destination.startsWith(MEMBER_TOPIC)) {
            Long memberId = parseId(destination, MEMBER_TOPIC);
            if (!memberId.equals(principal.memberId()) && principal.role() != MemberRole.ROLE_ADMIN) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        if ("/topic/realtime/admin".equals(destination)
            && principal.role() == MemberRole.ROLE_ADMIN) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private MemberPrincipal principal(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)
            || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return principal;
    }

    private Long parseId(String destination, String prefix) {
        try {
            return Long.valueOf(destination.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
