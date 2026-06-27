package io.mywallet.infrastructure.admin;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kill_switch")
public class KillSwitchEntity {

    public static final String SINGLETON_ID = "GLOBAL";

    @Id
    private String id;

    private boolean enabled;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    private String reason;

    protected KillSwitchEntity() {
        // JPA
    }

    public void activate(UUID activatedBy, String reason) {
        this.enabled = true;
        this.activatedBy = activatedBy;
        this.activatedAt = Instant.now();
        this.reason = reason;
    }

    public void deactivate() {
        this.enabled = false;
        this.activatedBy = null;
        this.activatedAt = null;
        this.reason = null;
    }

    public boolean isEnabled() { return enabled; }
    public UUID getActivatedBy() { return activatedBy; }
    public Instant getActivatedAt() { return activatedAt; }
    public String getReason() { return reason; }
}
