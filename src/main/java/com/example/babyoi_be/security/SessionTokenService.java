package com.example.babyoi_be.security;

import com.example.babyoi_be.domain.entity.UserSessionToken;
import com.example.babyoi_be.domain.entity.Users;
import com.example.babyoi_be.repository.UserSessionTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionTokenService {

    private static final Duration SESSION_TIMEOUT = Duration.ofDays(5);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final UserSessionTokenRepository userSessionTokenRepository;

    @Transactional
    public String issueToken(Users user) {
        userSessionTokenRepository.deleteAllByUser(user);

        String rawToken = generateRawToken();
        Instant now = Instant.now();

        UserSessionToken sessionToken = UserSessionToken.builder()
                .tokenHash(hashToken(rawToken))
                .user(user)
                .issuedAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plus(SESSION_TIMEOUT))
                .build();

        userSessionTokenRepository.save(sessionToken);
        return rawToken;
    }

    @Transactional
    public Optional<Users> authenticate(String rawToken) {
        Optional<UserSessionToken> sessionOptional = userSessionTokenRepository.findByTokenHash(hashToken(rawToken));
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }

        UserSessionToken sessionToken = sessionOptional.get();
        Instant now = Instant.now();

        if (sessionToken.getExpiresAt().isBefore(now)) {
            userSessionTokenRepository.delete(sessionToken);
            return Optional.empty();
        }

        sessionToken.setLastUsedAt(now);
        sessionToken.setExpiresAt(now.plus(SESSION_TIMEOUT));
        return Optional.of(sessionToken.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        userSessionTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(userSessionTokenRepository::delete);
    }

    @Transactional
    public void revokeAllByUser(Users user) {
        userSessionTokenRepository.deleteAllByUser(user);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void deleteExpiredTokens() {
        userSessionTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return TOKEN_ENCODER.encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
