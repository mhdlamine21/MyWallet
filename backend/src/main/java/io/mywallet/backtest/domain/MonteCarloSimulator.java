package io.mywallet.backtest.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bootstrap Monte Carlo: resamples the backtest's own observed period returns (with
 * replacement) to generate {@code numberOfSimulations} alternative equity paths of the
 * same length, then reports percentile bands at each tick. This answers "how much did
 * this specific result depend on the exact sequence of returns?" - a single backtest run
 * is one draw from a distribution of possible outcomes; resampling the same return pool
 * in different orders (and with repeats) approximates that distribution without assuming
 * any particular statistical shape (no normality assumption, unlike a purely analytic
 * confidence interval).
 *
 * <p>Deterministic given a seed - the same seed always produces the same simulated paths,
 * which is what makes this testable and what the project's "seedable" requirement for
 * reproducible backtests extends to.</p>
 */
public final class MonteCarloSimulator {

    private MonteCarloSimulator() {
    }

    public record ConfidenceBand(
        List<BigDecimal> p5,
        List<BigDecimal> p50,
        List<BigDecimal> p95
    ) {
    }

    public static ConfidenceBand simulate(
        BigDecimal initialCapital, List<BigDecimal> observedPeriodReturns,
        int numberOfPeriods, int numberOfSimulations, long seed
    ) {
        if (observedPeriodReturns.isEmpty()) {
            List<BigDecimal> flat = List.of(initialCapital);
            return new ConfidenceBand(flat, flat, flat);
        }

        org.apache.commons.math3.random.MersenneTwister random = new org.apache.commons.math3.random.MersenneTwister(seed);
        double[] returnPool = observedPeriodReturns.stream().mapToDouble(BigDecimal::doubleValue).toArray();

        // simulatedEquities[s][t] = equity value of simulation `s` at tick `t`
        double[][] simulatedEquities = new double[numberOfSimulations][numberOfPeriods + 1];
        double initial = initialCapital.doubleValue();

        for (int s = 0; s < numberOfSimulations; s++) {
            double equity = initial;
            simulatedEquities[s][0] = equity;
            for (int t = 1; t <= numberOfPeriods; t++) {
                double sampledReturn = returnPool[random.nextInt(returnPool.length)];
                equity *= (1 + sampledReturn);
                simulatedEquities[s][t] = equity;
            }
        }

        List<BigDecimal> p5 = new ArrayList<>(numberOfPeriods + 1);
        List<BigDecimal> p50 = new ArrayList<>(numberOfPeriods + 1);
        List<BigDecimal> p95 = new ArrayList<>(numberOfPeriods + 1);

        org.apache.commons.math3.stat.descriptive.rank.Percentile percentileCalc = 
            new org.apache.commons.math3.stat.descriptive.rank.Percentile();

        for (int t = 0; t <= numberOfPeriods; t++) {
            double[] valuesAtTick = new double[numberOfSimulations];
            for (int s = 0; s < numberOfSimulations; s++) {
                valuesAtTick[s] = simulatedEquities[s][t];
            }
            percentileCalc.setData(valuesAtTick);
            p5.add(BigDecimal.valueOf(percentileCalc.evaluate(5.0)).setScale(2, RoundingMode.HALF_UP));
            p50.add(BigDecimal.valueOf(percentileCalc.evaluate(50.0)).setScale(2, RoundingMode.HALF_UP));
            p95.add(BigDecimal.valueOf(percentileCalc.evaluate(95.0)).setScale(2, RoundingMode.HALF_UP));
        }

        return new ConfidenceBand(p5, p50, p95);
    }
}
