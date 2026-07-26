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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.config.SignupEmailVerificationProperties;
import com.ymall.backend.member.dto.SignupEmailVerificationConfirmResponse;
import com.ymall.backend.member.dto.SignupEmailVerificationResponse;
import com.ymall.backend.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class SignupEmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CHALLENGE_PREFIX = "signup-email:challenge:";
    private static final String TOKEN_PREFIX = "signup-email:token:";
    private static final String RATE_PREFIX = "signup-email:rate:";
    private static final String COOLDOWN_PREFIX = "signup-email:cooldown:";
    private static final DefaultRedisScript<String> VERIFY_SCRIPT = new DefaultRedisScript<>(
        """
        local storedEmailDigest = redis.call('HGET', KEYS[1], 'emailDigest')
        local storedCodeDigest = redis.call('HGET', KEYS[1], 'codeDigest')
        if not storedEmailDigest or storedEmailDigest ~= ARGV[2] or not storedCodeDigest then
            return nil
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts > tonumber(ARGV[3]) then
            redis.call('DEL', KEYS[1])
            return nil
        end
        if storedCodeDigest ~= ARGV[1] then
            if attempts >= tonumber(ARGV[3]) then
                redis.call('DEL', KEYS[1])
            end
            return nil
        end
        redis.call('DEL', KEYS[1])
        redis.call('SET', KEYS[2], storedEmailDigest, 'EX', ARGV[4])
        return 'verified'
        """,
        String.class
    );
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
        """
        local storedEmailDigest = redis.call('GET', KEYS[1])
        if not storedEmailDigest or storedEmailDigest ~= ARGV[1] then
            return 0
        end
        redis.call('DEL', KEYS[1])
        return 1
        """,
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final MemberRepository memberRepository;
    private final SignupEmailVerificationProperties properties;

    @Value("${ymall.mail.from}")
    private String from;

    public SignupEmailVerificationResponse send(String requestedEmail) {
        String email = normalize(requestedEmail);
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        String emailDigest = digest(email);
        enforceRequestLimits(emailDigest);

        String requestId = generateToken();
        String code = generateCode();
        String challengeKey = CHALLENGE_PREFIX + digest(requestId);
        redisTemplate.opsForHash().putAll(challengeKey, Map.of(
            "emailDigest", emailDigest,
            "codeDigest", digest(requestId + ":" + code),
            "attempts", "0"
        ));
        redisTemplate.expire(challengeKey, properties.getCodeTtl());

        try {
            sendEmail(email, code);
        } catch (BusinessException exception) {
            redisTemplate.delete(challengeKey);
            throw exception;
        }
        return new SignupEmailVerificationResponse(
            requestId,
            properties.getCodeTtl().toSeconds()
        );
    }

    public SignupEmailVerificationConfirmResponse confirm(
        String requestId,
        String requestedEmail,
        String code
    ) {
        String email = normalize(requestedEmail);
        String emailDigest = digest(email);
        String verificationToken = generateToken();
        String verified = redisTemplate.execute(
            VERIFY_SCRIPT,
            List.of(
                CHALLENGE_PREFIX + digest(requestId),
                TOKEN_PREFIX + digest(verificationToken)
            ),
            digest(requestId + ":" + code),
            emailDigest,
            Integer.toString(properties.getMaxAttempts()),
            Long.toString(properties.getTokenTtl().toSeconds())
        );
        if (verified == null) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_FAILED);
        }
        return new SignupEmailVerificationConfirmResponse(
            verificationToken,
            properties.getTokenTtl().toSeconds()
        );
    }

    public void consume(String verificationToken, String requestedEmail) {
        String email = normalize(requestedEmail);
        Long consumed = redisTemplate.execute(
            CONSUME_SCRIPT,
            List.of(TOKEN_PREFIX + digest(verificationToken)),
            digest(email)
        );
        if (consumed == null || consumed != 1L) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_VERIFICATION_REQUIRED);
        }
    }

    private void enforceRequestLimits(String emailDigest) {
        String rateKey = RATE_PREFIX + emailDigest;
        Long requestCount = redisTemplate.opsForValue().increment(rateKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateKey, properties.getRequestWindow());
        }
        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_PREFIX + emailDigest,
            "1",
            properties.getResendInterval()
        );
        if (requestCount == null
            || requestCount > properties.getMaxRequests()
            || !Boolean.TRUE.equals(cooldownStarted)) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("[YMall] 회원가입 이메일 인증번호");
        message.setText("YMall 회원가입 이메일 인증번호는 " + code + "입니다. 유효 시간 안에 입력해 주세요.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.SIGNUP_EMAIL_DELIVERY_FAILED);
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
