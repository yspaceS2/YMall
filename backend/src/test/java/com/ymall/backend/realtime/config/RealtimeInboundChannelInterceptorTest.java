package com.ymall.backend.realtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.global.security.MemberPrincipalResolver;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.repository.SupportInquiryRepository;

class RealtimeInboundChannelInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private MemberPrincipalResolver principalResolver;
    private SupportInquiryRepository inquiryRepository;
    private RealtimeInboundChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        principalResolver = mock(MemberPrincipalResolver.class);
        inquiryRepository = mock(SupportInquiryRepository.class);
        ObjectProvider<SupportInquiryRepository> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(inquiryRepository);
        interceptor = new RealtimeInboundChannelInterceptor(
            jwtTokenProvider,
            principalResolver,
            provider
        );
        when(principalResolver.resolve(any(MemberPrincipal.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        channel = mock(MessageChannel.class);
    }

    @Test
    void connectTokenIsStoredAsAuthenticatedUser() {
        MemberPrincipal principal = principal(7L, MemberRole.ROLE_USER);
        when(jwtTokenProvider.parseAccessToken("access-token")).thenReturn(principal);
        when(principalResolver.resolve(principal)).thenReturn(principal);
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer access-token");
        Message<byte[]> message = message(accessor);

        interceptor.preSend(message, channel);

        assertThat(accessor.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(((UsernamePasswordAuthenticationToken) accessor.getUser()).getPrincipal())
            .isEqualTo(principal);
    }

    @Test
    void memberCannotSubscribeToAnotherMembersInquiry() {
        MemberPrincipal principal = principal(7L, MemberRole.ROLE_USER);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/support/inquiries/31");
        accessor.setUser(authentication(principal));
        when(inquiryRepository.findByIdAndMemberId(31L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void memberCanSubscribeToOwnedInquiry() {
        MemberPrincipal principal = principal(7L, MemberRole.ROLE_USER);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/support/inquiries/31");
        accessor.setUser(authentication(principal));
        when(inquiryRepository.findByIdAndMemberId(31L, 7L))
            .thenReturn(Optional.of(mock(SupportInquiry.class)));

        assertThat(interceptor.preSend(message(accessor), channel)).isNotNull();
    }

    @Test
    void managerCanSubscribeToAdminAndSupportTopicsButNotAnotherMemberTopic() {
        MemberPrincipal manager = adminPrincipal(7L, AdminGrade.MANAGER);

        StompHeaderAccessor adminAccessor = accessor(StompCommand.SUBSCRIBE);
        adminAccessor.setDestination("/topic/realtime/admin");
        adminAccessor.setUser(authentication(manager));
        assertThat(interceptor.preSend(message(adminAccessor), channel)).isNotNull();

        StompHeaderAccessor inquiryAccessor = accessor(StompCommand.SUBSCRIBE);
        inquiryAccessor.setDestination("/topic/support/inquiries/31");
        inquiryAccessor.setUser(authentication(manager));
        when(inquiryRepository.existsById(31L)).thenReturn(true);
        assertThat(interceptor.preSend(message(inquiryAccessor), channel)).isNotNull();

        StompHeaderAccessor memberAccessor = accessor(StompCommand.SUBSCRIBE);
        memberAccessor.setDestination("/topic/realtime/members/8");
        memberAccessor.setUser(authentication(manager));
        assertThatThrownBy(() -> interceptor.preSend(message(memberAccessor), channel))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void superAdminCanSubscribeToAnotherMemberTopic() {
        MemberPrincipal superAdmin = adminPrincipal(7L, AdminGrade.SUPER_ADMIN);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/realtime/members/8");
        accessor.setUser(authentication(superAdmin));

        assertThat(interceptor.preSend(message(accessor), channel)).isNotNull();
    }

    @Test
    void subscriptionRejectsConnectionPrincipalInvalidatedAfterRoleChange() {
        MemberPrincipal stalePrincipal = adminPrincipal(7L, AdminGrade.SUPER_ADMIN);
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/realtime/admin");
        accessor.setUser(authentication(stalePrincipal));
        when(principalResolver.resolve(stalePrincipal))
            .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void sendRejectsConnectionPrincipalInvalidatedAfterRoleChange() {
        MemberPrincipal stalePrincipal = adminPrincipal(7L, AdminGrade.MANAGER);
        StompHeaderAccessor accessor = accessor(StompCommand.SEND);
        accessor.setDestination("/app/support/inquiries/31/messages");
        accessor.setUser(authentication(stalePrincipal));
        when(principalResolver.resolve(stalePrincipal))
            .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), channel))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private StompHeaderAccessor accessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private UsernamePasswordAuthenticationToken authentication(MemberPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            List.of(new SimpleGrantedAuthority(principal.role().name()))
        );
    }

    private MemberPrincipal principal(Long memberId, MemberRole role) {
        return new MemberPrincipal(memberId, "member@example.test", role);
    }

    private MemberPrincipal adminPrincipal(Long memberId, AdminGrade grade) {
        return new MemberPrincipal(
            memberId,
            "admin@example.test",
            MemberRole.ROLE_ADMIN,
            0L,
            grade,
            grade.permissions()
        );
    }
}
