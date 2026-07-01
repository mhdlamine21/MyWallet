package io.mywallet.backtest.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.backtest.domain.BacktestEngine;
import io.mywallet.backtest.domain.BacktestOutcome;
import io.mywallet.backtest.domain.MonteCarloSimulator;
import io.mywallet.backtest.domain.PerformanceMetrics;
import io.mywallet.backtest.infrastructure.persistence.BacktestEntity;
import io.mywallet.backtest.infrastructure.persistence.BacktestJpaRepository;
import io.mywallet.backtest.infrastructure.persistence.BacktestResultEntity;
import io.mywallet.backtest.infrastructure.persistence.BacktestResultJpaRepository;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.parser.RuleParser;
import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;
import io.mywallet.strategy.infrastructure.persistence.StrategyJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Orchestrates a full backtest run: loads real historical {@code MarketPrice} rows for the
 * requested asset/period (the same simulated GBM data the live market uses - see
 * {@code MarketDataGeneratorService}), parses the strategy's stored rule expression, runs
 * {@link BacktestEngine}, then runs {@link MonteCarloSimulator} on the resulting equity
 * curve's own period returns, and persists everything as a {@link BacktestResultEntity}.
 */
@Service
public class RunBacktestService {

    private static final int MONTE_CARLO_SIMULATIONS = 1000;

    private final StrategyJpaRepository strategyRepository;
    private final MarketPriceJpaRepository marketPriceRepository;
    private final BacktestJpaRepository backtestRepository;
    private final BacktestResultJpaRepository backtestResultRepository;
    private final ObjectMapper objectMapper;

    public RunBacktestService(
        StrategyJpaRepository strategyRepository,
        MarketPriceJpaRepository marketPriceRepository,
        BacktestJpaRepository backtestRepository,
        BacktestResultJpaRepository backtestResultRepository,
        ObjectMapper objectMapper
    ) {
        this.strategyRepository = strategyRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.backtestRepository = backtestRepository;
        this.backtestResultRepository = backtestResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID runBacktest(
        UUID strategyId, UUID requestingUserId, UUID assetId, BigDecimal initialCapital,
        Instant periodStart, Instant periodEnd, BigDecimal feeRate, BigDecimal slippageRate, Long seed
    ) {
        StrategyEntity strategy = strategyRepository.findById(strategyId)
            .orElseThrow(() -> new NoSuchElementException("Strategy not found: " + strategyId));
        if (!strategy.getOwnerId().equals(requestingUserId)) {
            throw new NoSuchElementException("Strategy not found: " + strategyId);
        }

        List<MarketPriceEntity> priceRows = marketPriceRepository
            .findByAssetIdAndObservedAtBetweenOrderByObservedAtAsc(assetId, periodStart, periodEnd);
        if (priceRows.size() < 2) {
            throw new IllegalArgumentException(
                "Not enough price history for the requested period (found %d points, need at least 2)".formatted(priceRows.size()));
        }
        List<BigDecimal> prices = priceRows.stream().map(MarketPriceEntity::getPrice).toList();

        double periodsPerYear = derivePeriodsPerYear(priceRows);
        BooleanExpression rule = RuleParser.parse(strategy.getRuleExpression());

        BacktestEngine.Config config = new BacktestEngine.Config(
            initialCapital, feeRate, slippageRate, periodsPerYear, BigDecimal.ZERO
        );
        BacktestOutcome outcome = BacktestEngine.run(prices, rule, config);

        long monteCarloSeed = seed != null ? seed : System.nanoTime();
        List<BigDecimal> equityReturns = PerformanceMetrics.periodReturns(outcome.equityCurve());
        MonteCarloSimulator.ConfidenceBand band = MonteCarloSimulator.simulate(
            initialCapital, equityReturns, prices.size() - 1, MONTE_CARLO_SIMULATIONS, monteCarloSeed
        );

        BacktestEntity backtest = new BacktestEntity(
            UUID.randomUUID(), strategyId, assetId, initialCapital, periodStart, periodEnd,
            feeRate, slippageRate, monteCarloSeed
        );
        backtestRepository.save(backtest);

        persistResult(backtest.getId(), outcome, band);
        return backtest.getId();
    }

    private double derivePeriodsPerYear(List<MarketPriceEntity> priceRows) {
        if (priceRows.size() < 2) {
            return 252; // fallback: assume daily-equivalent
        }
        Duration totalSpan = Duration.between(priceRows.get(0).getObservedAt(), priceRows.get(priceRows.size() - 1).getObservedAt());
        long totalSeconds = Math.max(1, totalSpan.getSeconds());
        double avgIntervalSeconds = (double) totalSeconds / (priceRows.size() - 1);
        double secondsPerYear = 365.0 * 24 * 3600;
        return secondsPerYear / avgIntervalSeconds;
    }

    private void persistResult(UUID backtestId, BacktestOutcome outcome, MonteCarloSimulator.ConfidenceBand band) {
        try {
            BacktestResultEntity result = new BacktestResultEntity(
                UUID.randomUUID(), backtestId, outcome.finalCapital(), outcome.totalReturn(), outcome.annualizedReturn(),
                outcome.numberOfTrades(), outcome.winRate(), outcome.averageGain(), outcome.averageLoss(),
                outcome.maxDrawdown(), outcome.sharpeRatio(), outcome.sortinoRatio(),
                objectMapper.writeValueAsString(outcome.equityCurve()),
                objectMapper.writeValueAsString(outcome.buyAndHoldEquityCurve()),
                objectMapper.writeValueAsString(band.p5()),
                objectMapper.writeValueAsString(band.p50()),
                objectMapper.writeValueAsString(band.p95()),
                objectMapper.writeValueAsString(outcome.trades())
            );
            backtestResultRepository.save(result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize backtest result", e);
        }
    }
}
