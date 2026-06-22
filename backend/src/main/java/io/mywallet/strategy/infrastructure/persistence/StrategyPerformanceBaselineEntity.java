package io.mywallet.strategy.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strategy_performance_baselines")
public class StrategyPerformanceBaselineEntity {

    @Id
    @Column(name = "strategy_id")
    private UUID strategyId;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "baseline_value", nullable = false)
    private BigDecimal baselineValue;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    protected StrategyPerformanceBaselineEntity() {
        // JPA
    }

    public StrategyPerformanceBaselineEntity(UUID strategyId, UUID portfolioId, BigDecimal baselineValue) {
        this.strategyId = strategyId;
        this.portfolioId = portfolioId;
        this.baselineValue = baselineValue;
        this.activatedAt = Instant.now();
    }

    public UUID getStrategyId() { return strategyId; }
    public UUID getPortfolioId() { return portfolioId; }
    public BigDecimal getBaselineValue() { return baselineValue; }
    public Instant getActivatedAt() { return activatedAt; }
}
