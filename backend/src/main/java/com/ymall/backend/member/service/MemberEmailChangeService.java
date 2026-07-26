package com.ymall.backend.member.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.config.EmailChangeProperties;
import com.ymall.backend.member.dto.EmailChangeReauthenticationResponse;
import com.ymall.backend.member.dto.EmailChangeVerificationResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.event.MemberEmailChangedEvent;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberEmailChangeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String NEW_CHALLENGE_PREFIX = "email-change:new:";
    private static final String REAUTHENTICATION_PREFIX = "email-change:reauthenticated:";
    private static final String RATE_PREFIX = "email-change:rate:";
    private static final String COOLDOWN_PREFIX = "email-change:cooldown:";
    private static final String PASSWORD_ATTEMPT_PREFIX = "email-change:password-attempt:";
    private static final int MAX_PASSWORD_ATTEMPTS = 10;
    private static final DefaultRedisScript<String> VERIFY_NEW_SCRIPT = new DefaultRedisScript<>(
        """
        local storedMemberId = redis.call('HGET', KEYS[1], 'memberId')
        local storedEmailDigest = redis.call('HGET', KEYS[1], 'emailDigest')
        local storedCodeDigest = redis.call('HGET', KEYS[1], 'codeDigest')
        if not storedMemberId or storedMemberId ~= ARGV[2]
            or not storedEmailDigest or storedEmailDigest ~= ARGV[3]
            or not storedCodeDigest then
            return nil
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts > tonumber(ARGV[4]) then
            redis.call('DEL', KEYS[1])
            return nil
        end
        if storedCodeDigest ~= ARGV[1] then
            if attempts >= tonumber(ARGV[4]) then
                redis.call('DEL', KEYS[1])
            end
            return nil
        end
        redis.call('DEL', KEYS[1])
        return 'verified'
        """,
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailChangeProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${ymall.mail.from}")
    private String from;

    public EmailChangeReauthenticationResponse reauthenticate(
        Long memberId,
        String currentPassword
    ) {
        Member member = findMember(memberId);
        if (member.hasPassword()) {
            enforcePasswordAttemptLimit(memberId);
            if (currentPassword == null
                || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, member.getPassword())) {
                throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
            }
            redisTemplate.delete(PASSWORD_ATTEMPT_PREFIX + memberId);
            markReauthenticated(member);
            return new EmailChangeReauthenticationResponse(
                false,
                null,
                null,
                properties.getReauthenticationTtl().toSeconds()
            );
        }
        throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
    }

    public void requireOAuthReauthentication(Long memberId) {
        if (findMember(memberId).hasPassword()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public void markOAuthReauthenticated(Long memberId) {
        markReauthenticated(findMember(memberId));
    }

    public EmailChangeVerificationResponse sendNewEmailVerification(
        Long memberId,
        String requestedEmail
    ) {
        Member member = findMember(memberId);
        requireReauthentication(member);
        String email = normalize(requestedEmail);
        validateNewEmail(member, email);
        enforceEmailRequestLimits("new", memberId, digest(email));

        String requestId = generateToken();
        String code = generateCode();
        String challengeKey = NEW_CHALLENGE_PREFIX + digest(requestId);
        redisTemplate.opsForHash().putAll(challengeKey, Map.of(
            "memberId", memberId.toString(),
            "emailDigest", digest(email),
            "codeDigest", digest(requestId + ":" + code),
            "attempts", "0"
        ));
        redisTemplate.expire(challengeKey, properties.getCodeTtl());
        try {
            sendCode(email, code, "새 이메일");
        } catch (BusinessException exception) {
            redisTemplate.delete(challengeKey);
            throw exception;
        }
        return new EmailChangeVerificationResponse(
            requestId,
            properties.getCodeTtl().toSeconds()
        );
    }

    @Transactional
    public void change(Long memberId, String requestId, String requestedEmail, String code) {
        String email = normalize(requestedEmail);
        String verified = redisTemplate.execute(
            VERIFY_NEW_SCRIPT,
            List.of(NEW_CHALLENGE_PREFIX + digest(requestId)),
            digest(requestId + ":" + code),
            memberId.toString(),
            digest(email),
            Integer.toString(properties.getMaxAttempts())
        );
        if (verified == null) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_VERIFICATION_FAILED);
        }

        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String reauthenticatedEmailDigest = redisTemplate.opsForValue()
            .getAndDelete(reauthenticationKey(memberId));
        if (!digest(member.getEmail()).equals(reauthenticatedEmailDigest)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
        validateNewEmail(member, email);

        String previousEmail = member.getEmail();
        member.changeEmail(email);
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        refreshTokenService.revokeAll(memberId);
        eventPublisher.publishEvent(new MemberEmailChangedEvent(memberId, previousEmail, email));
    }

    private void markReauthenticated(Member member) {
        redisTemplate.opsForValue().set(
            reauthenticationKey(member.getId()),
            digest(member.getEmail()),
            properties.getReauthenticationTtl()
        );
    }

    private void requireReauthentication(Member member) {
        String emailDigest = redisTemplate.opsForValue().get(reauthenticationKey(member.getId()));
        if (!digest(member.getEmail()).equals(emailDigest)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
    }

    private void validateNewEmail(Member member, String email) {
        if (member.getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_SAME_AS_CURRENT);
        }
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
    }

    private void enforcePasswordAttemptLimit(Long memberId) {
        String key = PASSWORD_ATTEMPT_PREFIX + memberId;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, properties.getRequestWindow());
        }
        if (attempts == null || attempts > MAX_PASSWORD_ATTEMPTS) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private void enforceEmailRequestLimits(String purpose, Long memberId, String emailDigest) {
        String suffix = purpose + ":" + memberId + ":" + emailDigest;
        String rateKey = RATE_PREFIX + suffix;
        Long requestCount = redisTemplate.opsForValue().increment(rateKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateKey, properties.getRequestWindow());
        }
        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_PREFIX + suffix,
            "1",
            properties.getResendInterval()
        );
        if (requestCount == null
            || requestCount > properties.getMaxRequests()
            || !Boolean.TRUE.equals(cooldownStarted)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void sendCode(String email, String code, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[YMall] 이메일 변경 " + purpose + " 인증번호");
        message.setText("YMall 이메일 변경 인증번호는 " + code + "입니다. 유효 시간 안에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_DELIVERY_FAILED);
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String reauthenticationKey(Long memberId) {
        return REAUTHENTICATION_PREFIX + memberId;
    }
}
