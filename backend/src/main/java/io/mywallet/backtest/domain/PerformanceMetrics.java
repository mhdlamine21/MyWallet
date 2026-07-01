package io.mywallet.backtest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure performance calculations over an equity curve or a period-return series. Computed
 * in {@code double} internally (statistical math, not money accounting - same rule as
 * {@code GbmPriceModel} and {@code TechnicalIndicators}), converted to {@code BigDecimal}
 * only at the boundary.
 */
public final class PerformanceMetrics {

    private PerformanceMetrics() {
    }

    /** Period-over-period returns from an equity curve: {@code (v[i] - v[i-1]) / v[i-1]}. */
    public static List<BigDecimal> periodReturns(List<BigDecimal> equityCurve) {
        if (equityCurve.size() < 2) {
            return List.of();
        }
        return java.util.stream.IntStream.range(1, equityCurve.size())
            .mapToObj(i -> {
                BigDecimal previous = equityCurve.get(i - 1);
                BigDecimal current = equityCurve.get(i);
                if (previous.signum() == 0) {
                    return BigDecimal.ZERO;
                }
                return current.subtract(previous).divide(previous, 10, RoundingMode.HALF_UP);
            })
            .toList();
    }

    public static BigDecimal totalReturn(BigDecimal initialCapital, BigDecimal finalCapital) {
        if (initialCapital.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return finalCapital.subtract(initialCapital).divide(initialCapital, 8, RoundingMode.HALF_UP);
    }

    /**
     * Annualizes a total return over {@code numberOfPeriods} periods, given how many such
     * periods occur in a year ({@code periodsPerYear}) - e.g. 252 for daily data, or
     * derived from the actual average tick interval for irregular data.
     */
    public static BigDecimal annualizedReturn(BigDecimal totalReturn, int numberOfPeriods, double periodsPerYear) {
        if (numberOfPeriods <= 0) {
            return BigDecimal.ZERO;
        }
        double growth = 1 + totalReturn.doubleValue();
        if (growth <= 0) {
            // Total loss or worse - annualizing a non-positive growth factor via a
            // fractional power is mathematically undefined (would need complex numbers).
            // Report -100% rather than NaN.
            return new BigDecimal("-1.00000000");
        }
        double annualized = Math.pow(growth, periodsPerYear / numberOfPeriods) - 1;
        return BigDecimal.valueOf(annualized).setScale(8, RoundingMode.HALF_UP);
    }

    /** Maximum peak-to-trough decline observed anywhere along the equity curve, as a positive fraction. */
    public static BigDecimal maxDrawdown(List<BigDecimal> equityCurve) {
        if (equityCurve.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double peak = equityCurve.get(0).doubleValue();
        double worstDrawdown = 0;
        for (BigDecimal pointBd : equityCurve) {
            double point = pointBd.doubleValue();
            peak = Math.max(peak, point);
            if (peak > 0) {
                double drawdown = (peak - point) / peak;
                worstDrawdown = Math.max(worstDrawdown, drawdown);
            }
        }
        return BigDecimal.valueOf(worstDrawdown).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Annualized Sharpe ratio: mean excess period return over its standard deviation,
     * scaled by {@code sqrt(periodsPerYear)}. Uses sample standard deviation (n-1).
     * Returns 0 if there is no variance (avoids a division-by-zero producing infinity).
     * Powered by Apache Commons Math DescriptiveStatistics.
     */
    public static BigDecimal sharpeRatio(List<BigDecimal> periodReturns, BigDecimal riskFreeRatePerPeriod, double periodsPerYear) {
        if (periodReturns.size() < 2) {
            return BigDecimal.ZERO;
        }
        double riskFree = riskFreeRatePerPeriod.doubleValue();
        double[] excessReturns = periodReturns.stream().mapToDouble(r -> r.doubleValue() - riskFree).toArray();
        org.apache.commons.math3.stat.descriptive.DescriptiveStatistics stats = 
            new org.apache.commons.math3.stat.descriptive.DescriptiveStatistics(excessReturns);
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        if (stdDev == 0) {
            return BigDecimal.ZERO;
        }
        double sharpe = (mean / stdDev) * Math.sqrt(periodsPerYear);
        return BigDecimal.valueOf(sharpe).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Annualized Sortino ratio: like Sharpe, but the denominator only penalizes downside
     * volatility (deviations below the minimum acceptable return, here 0 excess return)
     * rather than volatility in both directions.
     */
    public static BigDecimal sortinoRatio(List<BigDecimal> periodReturns, BigDecimal riskFreeRatePerPeriod, double periodsPerYear) {
        if (periodReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double riskFree = riskFreeRatePerPeriod.doubleValue();
        double[] excessReturns = periodReturns.stream().mapToDouble(r -> r.doubleValue() - riskFree).toArray();
        org.apache.commons.math3.stat.descriptive.DescriptiveStatistics stats = 
            new org.apache.commons.math3.stat.descriptive.DescriptiveStatistics(excessReturns);
        double mean = stats.getMean();

        double sumSquaredDownside = 0;
        for (double r : excessReturns) {
            if (r < 0) {
                sumSquaredDownside += r * r;
            }
        }
        double downsideDeviation = Math.sqrt(sumSquaredDownside / excessReturns.length);
        if (downsideDeviation == 0) {
            return BigDecimal.ZERO;
        }
        double sortino = (mean / downsideDeviation) * Math.sqrt(periodsPerYear);
        return BigDecimal.valueOf(sortino).setScale(4, RoundingMode.HALF_UP);
    }
}
