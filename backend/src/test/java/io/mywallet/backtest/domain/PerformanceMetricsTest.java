package io.mywallet.backtest.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PerformanceMetricsTest {

    private static List<BigDecimal> curve(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void periodReturnsComputesSimplePercentChanges() {
        var returns = PerformanceMetrics.periodReturns(curve(100, 110, 99));
        assertThat(returns).hasSize(2);
        assertThat(returns.get(0)).isEqualByComparingTo("0.1000000000");   // +10%
        assertThat(returns.get(1).doubleValue()).isCloseTo(-0.1, within(1e-9)); // -10%
    }

    @Test
    void totalReturnMatchesSimplePercentGrowth() {
        assertThat(PerformanceMetrics.totalReturn(new BigDecimal("1000"), new BigDecimal("1210")))
            .isEqualByComparingTo("0.21000000");
    }

    @Test
    void maxDrawdownFindsTheWorstPeakToTroughDecline() {
        // Peak at 150 (index 2), trough at 90 (index 4) -> drawdown = (150-90)/150 = 0.40
        var equity = curve(100, 120, 150, 130, 90, 110);
        assertThat(PerformanceMetrics.maxDrawdown(equity).doubleValue()).isCloseTo(0.40, within(1e-6));
    }

    @Test
    void maxDrawdownIsZeroForAMonotonicallyRisingCurve() {
        assertThat(PerformanceMetrics.maxDrawdown(curve(100, 110, 120, 130)).doubleValue()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void sharpeRatioMatchesHandComputedValue() {
        // returns = [0.02, -0.01, 0.03, 0.00], risk-free = 0
        // mean = 0.01, sample stdev = 0.018257419 (n-1), periodsPerYear = 252
        // sharpe = (0.01 / 0.018257419) * sqrt(252) ≈ 8.6949
        var returns = List.of(new BigDecimal("0.02"), new BigDecimal("-0.01"), new BigDecimal("0.03"), new BigDecimal("0.00"));
        BigDecimal sharpe = PerformanceMetrics.sharpeRatio(returns, BigDecimal.ZERO, 252);

        assertThat(sharpe.doubleValue()).isCloseTo(8.6949, within(0.01));
    }

    @Test
    void sharpeRatioIsZeroWhenThereIsNoVolatility() {
        var returns = List.of(new BigDecimal("0.01"), new BigDecimal("0.01"), new BigDecimal("0.01"));
        assertThat(PerformanceMetrics.sharpeRatio(returns, BigDecimal.ZERO, 252)).isEqualByComparingTo("0");
    }

    @Test
    void sortinoRatioIgnoresUpsideVolatility() {
        // Same mean as a volatile-both-ways series, but all deviation is upside ->
        // downside deviation is much smaller than Sharpe's full stdev -> Sortino > Sharpe.
        var returns = List.of(new BigDecimal("0.01"), new BigDecimal("0.05"), new BigDecimal("0.01"), new BigDecimal("0.05"));
        BigDecimal sharpe = PerformanceMetrics.sharpeRatio(returns, BigDecimal.ZERO, 252);
        BigDecimal sortino = PerformanceMetrics.sortinoRatio(returns, BigDecimal.ZERO, 252);

        // No negative excess returns at all here -> zero downside deviation -> Sortino reports 0 by convention.
        assertThat(sortino).isEqualByComparingTo("0");
        assertThat(sharpe.doubleValue()).isGreaterThan(0);
    }

    @Test
    void sortinoPenalizesActualDownsideVolatility() {
        var returns = List.of(new BigDecimal("0.03"), new BigDecimal("-0.03"), new BigDecimal("0.02"), new BigDecimal("-0.01"));
        BigDecimal sortino = PerformanceMetrics.sortinoRatio(returns, BigDecimal.ZERO, 252);
        assertThat(sortino.doubleValue()).isNotZero();
    }

    @Test
    void annualizedReturnCompoundsCorrectlyOverSubYearPeriod() {
        // 10% total return over 6 months (periodsPerYear=2 "half-years") -> should annualize to 21% (1.1^2 - 1)
        BigDecimal annualized = PerformanceMetrics.annualizedReturn(new BigDecimal("0.10"), 1, 2);
        assertThat(annualized.doubleValue()).isCloseTo(0.21, within(0.001));
    }

    @Test
    void annualizedReturnHandlesTotalLossWithoutThrowing() {
        BigDecimal annualized = PerformanceMetrics.annualizedReturn(new BigDecimal("-1.00"), 10, 252);
        assertThat(annualized).isEqualByComparingTo("-1.00000000");
    }
}
