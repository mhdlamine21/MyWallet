package io.mywallet.marketdata.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GbmPriceModelTest {

    private static final double ONE_DAY_IN_YEARS = 1.0 / 365.0;

    @Test
    void priceIsAlwaysPositiveRegardlessOfGaussianSample() {
        BigDecimal current = new BigDecimal("100.00");
        BigDecimal drift = new BigDecimal("0.05");
        BigDecimal volatility = new BigDecimal("0.80"); // deliberately high volatility

        // Even an extreme negative Gaussian draw must not push price to/below zero.
        BigDecimal result = GbmPriceModel.nextPrice(current, drift, volatility, ONE_DAY_IN_YEARS, -10.0);

        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void zeroVolatilityAndZeroDriftLeavesPriceUnchanged() {
        BigDecimal current = new BigDecimal("50.00000000");

        BigDecimal result = GbmPriceModel.nextPrice(current, BigDecimal.ZERO, BigDecimal.ZERO, ONE_DAY_IN_YEARS, 0.0);

        assertThat(result).isEqualByComparingTo(current);
    }

    @Test
    void sameSeedProducesTheSameSequenceOfPrices() {
        BigDecimal drift = new BigDecimal("0.08");
        BigDecimal volatility = new BigDecimal("0.30");

        BigDecimal priceA = simulateTenSteps(new Random(42L), drift, volatility);
        BigDecimal priceB = simulateTenSteps(new Random(42L), drift, volatility);

        assertThat(priceA).isEqualByComparingTo(priceB);
    }

    @Test
    void positiveGaussianSampleIncreasesPriceAndNegativeDecreasesIt() {
        BigDecimal current = new BigDecimal("100.00");
        BigDecimal drift = BigDecimal.ZERO;
        BigDecimal volatility = new BigDecimal("0.30");

        BigDecimal up = GbmPriceModel.nextPrice(current, drift, volatility, ONE_DAY_IN_YEARS, 1.0);
        BigDecimal down = GbmPriceModel.nextPrice(current, drift, volatility, ONE_DAY_IN_YEARS, -1.0);

        assertThat(up).isGreaterThan(current);
        assertThat(down).isLessThan(current);
    }

    @Test
    void rejectsNonPositiveCurrentPrice() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            GbmPriceModel.nextPrice(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.2"), ONE_DAY_IN_YEARS, 0.0)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    private BigDecimal simulateTenSteps(Random random, BigDecimal drift, BigDecimal volatility) {
        BigDecimal price = new BigDecimal("100.00");
        for (int i = 0; i < 10; i++) {
            price = GbmPriceModel.nextPrice(price, drift, volatility, ONE_DAY_IN_YEARS, random.nextGaussian());
        }
        return price;
    }
}
