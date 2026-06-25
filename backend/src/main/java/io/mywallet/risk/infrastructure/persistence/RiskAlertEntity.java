package io.mywallet.risk.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_alerts")
public class RiskAlertEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "limit_type", nullable = false)
    private String limitType;

    @Column(nullable = false)
    private String level;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private String explanation;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    protected RiskAlertEntity() {
        // JPA
    }

    public RiskAlertEntity(UUID id, UUID portfolioId, String limitType, String level, int riskScore, String explanation) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.limitType = limitType;
        this.level = level;
        this.riskScore = riskScore;
        this.explanation = explanation;
        this.raisedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public String getLimitType() { return limitType; }
    public String getLevel() { return level; }
    public int getRiskScore() { return riskScore; }
    public String getExplanation() { return explanation; }
    public Instant getRaisedAt() { return raisedAt; }
}
