package io.mywallet.backtest.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backtests")
public class BacktestEntity {

    @Id
    private UUID id;

    @Column(name = "strategy_id", nullable = false)
    private UUID strategyId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "initial_capital", nullable = false)
    private BigDecimal initialCapital;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "fee_rate", nullable = false)
    private BigDecimal feeRate;

    @Column(name = "slippage_rate", nullable = false)
    private BigDecimal slippageRate;

    @Column(name = "monte_carlo_seed", nullable = false)
    private long monteCarloSeed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BacktestEntity() {
        // JPA
    }

    public BacktestEntity(UUID id, UUID strategyId, UUID assetId, BigDecimal initialCapital,
                           Instant periodStart, Instant periodEnd, BigDecimal feeRate,
                           BigDecimal slippageRate, long monteCarloSeed) {
        this.id = id;
        this.strategyId = strategyId;
        this.assetId = assetId;
        this.initialCapital = initialCapital;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.feeRate = feeRate;
        this.slippageRate = slippageRate;
        this.monteCarloSeed = monteCarloSeed;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStrategyId() { return strategyId; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public BigDecimal getFeeRate() { return feeRate; }
    public BigDecimal getSlippageRate() { return slippageRate; }
    public long getMonteCarloSeed() { return monteCarloSeed; }
    public Instant getCreatedAt() { return createdAt; }
}
