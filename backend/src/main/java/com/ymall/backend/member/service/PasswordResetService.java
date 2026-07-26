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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.ymall.backend.member.config.PasswordResetProperties;
import com.ymall.backend.member.dto.PasswordResetConfirmRequest;
import com.ymall.backend.member.dto.PasswordResetRequestResponse;
import com.ymall.backend.member.dto.PasswordResetVerificationResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHALLENGE_KEY_PREFIX = "password-reset:challenge:";
    private static final String TOKEN_KEY_PREFIX = "password-reset:token:";
    private static final String RATE_KEY_PREFIX = "password-reset:rate:";
    private static final String COOLDOWN_KEY_PREFIX = "password-reset:cooldown:";
    private static final String DUMMY_MEMBER_ID = "0";
    private static final DefaultRedisScript<String> VERIFY_SCRIPT = new DefaultRedisScript<>(
        """
        local storedDigest = redis.call('HGET', KEYS[1], 'codeDigest')
        if not storedDigest then
            return nil
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts > tonumber(ARGV[2]) then
            redis.call('DEL', KEYS[1])
            return nil
        end
        if storedDigest ~= ARGV[1] then
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1])
            end
            return nil
        end
        local memberId = redis.call('HGET', KEYS[1], 'memberId')
        redis.call('DEL', KEYS[1])
        if not memberId or memberId == '0' then
            return nil
        end
        redis.call('SET', KEYS[2], memberId, 'EX', ARGV[3])
        return memberId
        """,
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetProperties properties;

    @Value("${ymall.mail.from}")
    private String from;

    public PasswordResetRequestResponse request(String requestedEmail) {
        String email = normalize(requestedEmail);
        String emailHash = digest(email);
        enforceRequestLimits(emailHash);

        String requestId = generateToken();
        String code = generateCode();
        String challengeKey = CHALLENGE_KEY_PREFIX + digest(requestId);
        String memberId = memberRepository.findByEmailIgnoreCase(email)
            .filter(Member::hasPassword)
            .map(member -> member.getId().toString())
            .orElse(DUMMY_MEMBER_ID);

        redisTemplate.opsForHash().putAll(challengeKey, Map.of(
            "memberId", memberId,
            "codeDigest", digest(requestId + ":" + code),
            "attempts", "0"
        ));
        redisTemplate.expire(challengeKey, properties.getCodeTtl());

        if (!DUMMY_MEMBER_ID.equals(memberId) && !sendEmail(email, code)) {
            redisTemplate.delete(challengeKey);
        }
        return new PasswordResetRequestResponse(requestId);
    }

    public PasswordResetVerificationResponse verify(String requestId, String code) {
        String resetToken = generateToken();
        String memberId = redisTemplate.execute(
            VERIFY_SCRIPT,
            List.of(
                CHALLENGE_KEY_PREFIX + digest(requestId),
                TOKEN_KEY_PREFIX + digest(resetToken)
            ),
            digest(requestId + ":" + code),
            Integer.toString(properties.getMaxAttempts()),
            Long.toString(properties.getResetTokenTtl().toSeconds())
        );
        if (memberId == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_VERIFICATION_FAILED);
        }
        return new PasswordResetVerificationResponse(
            resetToken,
            properties.getResetTokenTtl().toSeconds()
        );
    }

    @Transactional
    public void reset(PasswordResetConfirmRequest request) {
        String memberId = redisTemplate.opsForValue().getAndDelete(
            TOKEN_KEY_PREFIX + digest(request.resetToken())
        );
        if (memberId == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        Member member;
        try {
            member = memberRepository.findByIdForUpdate(Long.valueOf(memberId))
                .filter(Member::hasPassword)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(member.getId());
    }

    private void enforceRequestLimits(String emailHash) {
        String rateKey = RATE_KEY_PREFIX + emailHash;
        Long requestCount = redisTemplate.opsForValue().increment(rateKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateKey, properties.getRequestWindow());
        }

        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_KEY_PREFIX + emailHash,
            "1",
            properties.getResendInterval()
        );
        if (requestCount == null
            || requestCount > properties.getMaxRequests()
            || !Boolean.TRUE.equals(cooldownStarted)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private boolean sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[YMall] 비밀번호 재설정 인증번호");
        message.setText("YMall 비밀번호 재설정 인증번호는 " + code + "입니다. 유효 시간 안에 입력해 주세요.");
        try {
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            log.warn("Password reset email delivery failed");
            return false;
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
}
