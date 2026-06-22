package io.mywallet.strategy.infrastructure.persistence;

import io.mywallet.strategy.domain.InvalidStrategyTransitionException;
import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.domain.StrategyMode;
import io.mywallet.strategy.domain.StrategyStatus;
import jakarta.persistence.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Strategy is a plain versioned JPA entity (not event-sourced) - see
 * docs/architecture/domain-model.md for why. {@code ruleExpression} is validated by
 * {@code RuleParser.parse(...)} at the application-service layer before this entity is
 * ever persisted, so any row in this table is guaranteed parseable - this entity itself
 * doesn't re-validate on every load (that would be wasted work; the stored text is
 * trusted once it passed the gate on the way in).
 */
@Entity
@Table(name = "strategies")
public class StrategyEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyStatus status = StrategyStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StrategyMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Column(name = "maximum_capital")
    private BigDecimal maximumCapital;

    @Column(name = "maximum_loss")
    private BigDecimal maximumLoss;

    @Column(name = "rule_expression", nullable = false)
    private String ruleExpression;

    @jakarta.persistence.Version
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StrategyEntity() {
        // JPA
    }

    public StrategyEntity(UUID id, String name, String description, UUID ownerId, UUID portfolioId,
                           StrategyMode mode, RiskLevel riskLevel, BigDecimal maximumCapital,
                           BigDecimal maximumLoss, String ruleExpression) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.portfolioId = portfolioId;
        this.mode = mode;
        this.riskLevel = riskLevel;
        this.maximumCapital = maximumCapital;
        this.maximumLoss = maximumLoss;
        this.ruleExpression = ruleExpression;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void activate() {
        transitionTo(StrategyStatus.ACTIVE);
    }

    public void suspend() {
        transitionTo(StrategyStatus.SUSPENDED);
    }

    public void deactivate() {
        transitionTo(StrategyStatus.DEACTIVATED);
    }

    private void transitionTo(StrategyStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStrategyTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void assertOwnedBy(UUID userId) {
        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("Not authorized to modify this strategy");
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getPortfolioId() { return portfolioId; }
    public StrategyStatus getStatus() { return status; }
    public StrategyMode getMode() { return mode; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public BigDecimal getMaximumCapital() { return maximumCapital; }
    public BigDecimal getMaximumLoss() { return maximumLoss; }
    public String getRuleExpression() { return ruleExpression; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
