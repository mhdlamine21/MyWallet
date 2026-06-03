package io.mywallet.auth.infrastructure.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Refresh tokens are high-entropy random opaque strings, not JWTs - a slow adaptive hash
 * (BCrypt) would be needless overhead for validating them on every refresh call, so a
 * plain SHA-256 digest is used instead. This is safe specifically because the input has
 * ~256 bits of entropy (unlike a user-chosen password, which SHA-256 alone would be a bad
 * fit for).
 */
@Component
public class RefreshTokenCrypto {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM - this can never actually happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
