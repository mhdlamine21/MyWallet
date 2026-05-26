package io.mywallet.portfolio.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read projection of the Portfolio aggregate - NOT the source of truth (that's
 * {@code domain_events}). Kept in sync by {@code PortfolioProjector} whenever a Portfolio
 * event is appended. Exists so that "list my portfolios" and "get portfolio balance"
 * queries don't need to replay event history on every read.
 */
@Entity
@Table(name = "portfolio_projections")
public class PortfolioProjectionEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mode;

    @Column(name = "cash_balance", nullable = false)
    private BigDecimal cashBalance;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PortfolioProjectionEntity() {
        // JPA
    }

    public PortfolioProjectionEntity(UUID id, UUID accountId, String name, String mode,
                                      BigDecimal cashBalance, long version, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.name = name;
        this.mode = mode;
        this.cashBalance = cashBalance;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void updateCashBalance(BigDecimal newBalance, long newVersion) {
        this.cashBalance = newBalance;
        this.version = newVersion;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getName() { return name; }
    public String getMode() { return mode; }
    public BigDecimal getCashBalance() { return cashBalance; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
