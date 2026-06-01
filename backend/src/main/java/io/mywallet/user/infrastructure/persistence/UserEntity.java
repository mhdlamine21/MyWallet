package io.mywallet.user.infrastructure.persistence;

import io.mywallet.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    /** After this many consecutive failed login attempts, the account locks temporarily. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @ManyToMany(fetch = FetchType.EAGER) // roles are needed on every authenticated request
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    protected UserEntity() {
        // JPA
    }

    public UserEntity(UUID id, String email, String passwordHash) {
        super(id);
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    /** True if a prior lockout is currently in effect (i.e. {@code lockedUntil} is in the future). */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /**
     * Records one failed login attempt. Locks the account for
     * {@value #LOCKOUT_DURATION_MINUTES} minutes once {@value #MAX_FAILED_ATTEMPTS}
     * consecutive failures accumulate - a fixed lockout window rather than exponential
     * backoff, chosen for simplicity; the IP-based {@code AuthRateLimitFilter} is the
     * first line of defense against sustained brute force, this is the second, per-account
     * line that survives even a distributed attack across many IPs targeting one account.
     */
    public void recordFailedLogin(Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60);
        }
    }

    public void recordSuccessfulLogin() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }

    public void assignRole(RoleEntity role) {
        this.roles.add(role);
    }
}
