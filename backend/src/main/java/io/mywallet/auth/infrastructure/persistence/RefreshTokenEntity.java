package io.mywallet.auth.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token is stored only as its SHA-256 hash ({@code tokenHash}) - the raw value
 * is handed to the client once and never persisted, so a database leak alone does not let
 * an attacker mint new access tokens. Rotation is modeled explicitly: refreshing sets
 * {@code revokedAt} on the old row and links it to the new one via
 * {@code replacedById}, so reuse of an already-rotated token (a strong signal of theft) is
 * detectable - see {@code AuthenticationService#refresh}.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected RefreshTokenEntity() {
        // JPA
    }

    public RefreshTokenEntity(UUID id, UUID userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public void revoke(UUID replacedById) {
        this.revokedAt = Instant.now();
        this.replacedById = replacedById;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getReplacedById() { return replacedById; }
}
