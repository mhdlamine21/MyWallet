package io.mywallet.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates short-lived JWT <em>access</em> tokens. Refresh tokens are a
 * separate, opaque, database-backed concept (see {@code RefreshTokenService}) -
 * deliberately not JWTs themselves, so they can be revoked and rotated server-side
 * without waiting for expiry, which a self-contained JWT can't be.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenTtlSeconds;

    public JwtTokenProvider(
        @Value("${mywallet.jwt.secret}") String secret,
        @Value("${mywallet.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds
    ) {
        // HMAC-SHA key derived from the configured secret. In production this must be a
        // real random 64-byte value (see .env.example) - never the dev default.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String generateAccessToken(UUID userId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(now.plus(accessTokenTtlSeconds, ChronoUnit.SECONDS)))
            .signWith(signingKey)
            .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    /** Parses and validates a token, returning its claims - empty if invalid or expired. */
    public Optional<Claims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesFrom(Claims claims) {
        return (List<String>) claims.get("roles", List.class);
    }

    public UUID userIdFrom(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }
}
