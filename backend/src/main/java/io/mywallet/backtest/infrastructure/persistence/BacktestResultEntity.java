package io.mywallet.backtest.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backtest_results")
public class BacktestResultEntity {

    @Id
    private UUID id;

    @Column(name = "backtest_id", nullable = false)
    private UUID backtestId;

    @Column(name = "final_capital", nullable = false)
    private BigDecimal finalCapital;

    @Column(name = "total_return", nullable = false)
    private BigDecimal totalReturn;

    @Column(name = "annualized_return", nullable = false)
    private BigDecimal annualizedReturn;

    @Column(name = "number_of_trades", nullable = false)
    private int numberOfTrades;

    @Column(name = "win_rate", nullable = false)
    private BigDecimal winRate;

    @Column(name = "average_gain", nullable = false)
    private BigDecimal averageGain;

    @Column(name = "average_loss", nullable = false)
    private BigDecimal averageLoss;

    @Column(name = "max_drawdown", nullable = false)
    private BigDecimal maxDrawdown;

    @Column(name = "sharpe_ratio", nullable = false)
    private BigDecimal sharpeRatio;

    @Column(name = "sortino_ratio", nullable = false)
    private BigDecimal sortinoRatio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "equity_curve", nullable = false, columnDefinition = "jsonb")
    private String equityCurveJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "buy_and_hold_equity_curve", nullable = false, columnDefinition = "jsonb")
    private String buyAndHoldEquityCurveJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "monte_carlo_p5", nullable = false, columnDefinition = "jsonb")
    private String monteCarloP5Json;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "monte_carlo_p50", nullable = false, columnDefinition = "jsonb")
    private String monteCarloP50Json;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "monte_carlo_p95", nullable = false, columnDefinition = "jsonb")
    private String monteCarloP95Json;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String trades;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BacktestResultEntity() {
        // JPA
    }

    public BacktestResultEntity(
        UUID id, UUID backtestId, BigDecimal finalCapital, BigDecimal totalReturn, BigDecimal annualizedReturn,
        int numberOfTrades, BigDecimal winRate, BigDecimal averageGain, BigDecimal averageLoss,
        BigDecimal maxDrawdown, BigDecimal sharpeRatio, BigDecimal sortinoRatio,
        String equityCurveJson, String buyAndHoldEquityCurveJson,
        String monteCarloP5Json, String monteCarloP50Json, String monteCarloP95Json, String tradesJson
    ) {
        this.id = id;
        this.backtestId = backtestId;
        this.finalCapital = finalCapital;
        this.totalReturn = totalReturn;
        this.annualizedReturn = annualizedReturn;
        this.numberOfTrades = numberOfTrades;
        this.winRate = winRate;
        this.averageGain = averageGain;
        this.averageLoss = averageLoss;
        this.maxDrawdown = maxDrawdown;
        this.sharpeRatio = sharpeRatio;
        this.sortinoRatio = sortinoRatio;
        this.equityCurveJson = equityCurveJson;
        this.buyAndHoldEquityCurveJson = buyAndHoldEquityCurveJson;
        this.monteCarloP5Json = monteCarloP5Json;
        this.monteCarloP50Json = monteCarloP50Json;
        this.monteCarloP95Json = monteCarloP95Json;
        this.trades = tradesJson;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBacktestId() { return backtestId; }
    public BigDecimal getFinalCapital() { return finalCapital; }
    public BigDecimal getTotalReturn() { return totalReturn; }
    public BigDecimal getAnnualizedReturn() { return annualizedReturn; }
    public int getNumberOfTrades() { return numberOfTrades; }
    public BigDecimal getWinRate() { return winRate; }
    public BigDecimal getAverageGain() { return averageGain; }
    public BigDecimal getAverageLoss() { return averageLoss; }
    public BigDecimal getMaxDrawdown() { return maxDrawdown; }
    public BigDecimal getSharpeRatio() { return sharpeRatio; }
    public BigDecimal getSortinoRatio() { return sortinoRatio; }
    public String getEquityCurveJson() { return equityCurveJson; }
    public String getBuyAndHoldEquityCurveJson() { return buyAndHoldEquityCurveJson; }
    public String getMonteCarloP5Json() { return monteCarloP5Json; }
    public String getMonteCarloP50Json() { return monteCarloP50Json; }
    public String getMonteCarloP95Json() { return monteCarloP95Json; }
    public String getTradesJson() { return trades; }
    public Instant getCreatedAt() { return createdAt; }
}
