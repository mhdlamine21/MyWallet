package io.mywallet.marketdata.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure Geometric Brownian Motion price step, per the discovery decision "random walk
 * paramétrable par actif" - upgraded to GBM (rather than a plain random walk) during the
 * `/improve-architecture`-adjacent feature brainstorm, since GBM is the standard
 * quantitative-finance model and keeps prices strictly positive by construction.
 *
 * <p>Deliberately computed in {@code double}, not {@code BigDecimal}: this is a stochastic
 * <em>simulation</em> process (exp, sqrt, Gaussian sampling), not a money/ledger
 * calculation - the project's "always BigDecimal for money" rule applies to balances,
 * fees, and order quantities, not to generating a simulated price tick. The result is
 * converted to {@code BigDecimal} at the boundary, once, for storage.</p>
 */
public final class GbmPriceModel {

    private GbmPriceModel() {
    }

    /**
     * @param currentPrice   the price at the start of this step (must be positive)
     * @param annualDrift    annualized expected return (mu), e.g. 0.08 for 8%/year
     * @param annualVolatility annualized volatility (sigma), e.g. 0.30 for 30%/year
     * @param dtYears        elapsed time for this step, expressed in years (e.g. a 5-second
     *                       tick is {@code 5.0 / (365 * 24 * 3600)})
     * @param gaussianSample a draw from a standard normal distribution (mean 0, stddev 1) -
     *                       passed in rather than sampled internally so this method stays a
     *                       pure function, trivially testable with fixed inputs
     */
    public static BigDecimal nextPrice(
        BigDecimal currentPrice,
        BigDecimal annualDrift,
        BigDecimal annualVolatility,
        double dtYears,
        double gaussianSample
    ) {
        if (currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("currentPrice must be positive, was: " + currentPrice);
        }

        double s = currentPrice.doubleValue();
        double mu = annualDrift.doubleValue();
        double sigma = annualVolatility.doubleValue();

        double drift = (mu - 0.5 * sigma * sigma) * dtYears;
        double diffusion = sigma * Math.sqrt(dtYears) * gaussianSample;
        double next = s * Math.exp(drift + diffusion);

        // GBM is mathematically always > 0, but guard against floating-point underflow
        // over pathological inputs (e.g. extreme volatility) reaching exactly zero, which
        // would make every subsequent tick permanently zero (exp(anything) * 0 = 0).
        next = Math.max(next, 1e-8);

        return BigDecimal.valueOf(next).setScale(8, RoundingMode.HALF_UP);
    }
}
