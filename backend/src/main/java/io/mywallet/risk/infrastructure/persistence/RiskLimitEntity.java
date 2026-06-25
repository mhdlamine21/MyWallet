package io.mywallet.risk.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_limits")
public class RiskLimitEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "limit_type", nullable = false)
    private String limitType;

    @Column(nullable = false)
    private BigDecimal threshold;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RiskLimitEntity() {
        // JPA
    }

    public RiskLimitEntity(UUID id, UUID portfolioId, String limitType, BigDecimal threshold) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.limitType = limitType;
        this.threshold = threshold;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public String getLimitType() { return limitType; }
    public BigDecimal getThreshold() { return threshold; }
}
