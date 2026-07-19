package com.ymall.backend.global.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.ymall.backend.member.entity.OAuthProvider;

@Component
public class OAuthFlowContext {

    private static final String SESSION_KEY = OAuthFlowContext.class.getName() + ".request";
    private static final String LINK_SESSION_KEY = OAuthFlowContext.class.getName() + ".link";
    private static final String EMAIL_SESSION_KEY = OAuthFlowContext.class.getName() + ".email";

    public void startLink(HttpServletRequest request, Long memberId, OAuthProvider provider) {
        request.getSession(true).setAttribute(
            LINK_SESSION_KEY,
            new LinkRequest(memberId, provider, Instant.now().plus(5, ChronoUnit.MINUTES))
        );
    }

    public Optional<Long> consumeLink(OAuthProvider provider) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
            .getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null || !(session.getAttribute(LINK_SESSION_KEY) instanceof LinkRequest link)) {
            return Optional.empty();
        }
        session.removeAttribute(LINK_SESSION_KEY);
        if (link.provider() != provider || link.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(link.memberId());
    }

    public void start(HttpServletRequest request, OAuthProvider provider, OAuth2UserProfile profile) {
        request.getSession(true).setAttribute(
            SESSION_KEY,
            new SignupRequest(provider, profile, Instant.now().plus(5, ChronoUnit.MINUTES))
        );
    }

    public Optional<PendingSignup> get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_KEY) instanceof SignupRequest signup)) {
            return Optional.empty();
        }
        if (signup.expiresAt().isBefore(Instant.now())) {
            session.removeAttribute(SESSION_KEY);
            return Optional.empty();
        }
        return Optional.of(new PendingSignup(signup.provider(), signup.profile()));
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
            session.removeAttribute(EMAIL_SESSION_KEY);
        }
    }

    public void startEmailVerification(HttpServletRequest request, String email, String code) {
        request.getSession(true).setAttribute(
            EMAIL_SESSION_KEY,
            new EmailVerification(email, code, false, 0, Instant.now().plus(5, ChronoUnit.MINUTES))
        );
    }

    public boolean verifyEmail(HttpServletRequest request, String email, String code) {
        HttpSession session = request.getSession(false);
        if (session == null
            || !(session.getAttribute(EMAIL_SESSION_KEY) instanceof EmailVerification verification)
            || verification.expiresAt().isBefore(Instant.now())
            || verification.attempts() >= 5
            || !verification.email().equals(email)) {
            return false;
        }
        boolean verified = verification.code().equals(code);
        session.setAttribute(
            EMAIL_SESSION_KEY,
            new EmailVerification(
                verification.email(),
                verification.code(),
                verified,
                verification.attempts() + 1,
                verification.expiresAt()
            )
        );
        return verified;
    }

    public Optional<String> getVerifiedEmail(HttpServletRequest request, String email) {
        HttpSession session = request.getSession(false);
        if (session == null
            || !(session.getAttribute(EMAIL_SESSION_KEY) instanceof EmailVerification verification)
            || !verification.verified()
            || verification.expiresAt().isBefore(Instant.now())
            || !verification.email().equals(email)) {
            return Optional.empty();
        }
        return Optional.of(verification.email());
    }

    public record PendingSignup(OAuthProvider provider, OAuth2UserProfile profile) {
    }

    private record SignupRequest(
        OAuthProvider provider,
        OAuth2UserProfile profile,
        Instant expiresAt
    ) {
    }

    private record LinkRequest(Long memberId, OAuthProvider provider, Instant expiresAt) {
    }

    private record EmailVerification(
        String email,
        String code,
        boolean verified,
        int attempts,
        Instant expiresAt
    ) {
    }
}
