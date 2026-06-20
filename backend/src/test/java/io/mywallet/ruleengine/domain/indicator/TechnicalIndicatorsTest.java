package io.mywallet.ruleengine.domain.indicator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TechnicalIndicatorsTest {

    private static List<BigDecimal> prices(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void smaOfConstantPricesEqualsThatConstant() {
        List<BigDecimal> flat = prices(100, 100, 100, 100, 100);
        assertThat(TechnicalIndicators.sma(flat, 5)).isEqualByComparingTo("100.00000000");
    }

    @Test
    void smaMatchesHandComputedAverage() {
        // last 3 of [1,2,3,4,5] = [3,4,5] -> avg = 4
        List<BigDecimal> series = prices(1, 2, 3, 4, 5);
        assertThat(TechnicalIndicators.sma(series, 3)).isEqualByComparingTo("4");
    }

    @Test
    void smaThrowsWhenNotEnoughData() {
        assertThatThrownBy(() -> TechnicalIndicators.sma(prices(1, 2), 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emaOfConstantPricesEqualsThatConstant() {
        List<BigDecimal> flat = prices(50, 50, 50, 50, 50, 50, 50, 50, 50, 50);
        assertThat(TechnicalIndicators.ema(flat, 5)).isEqualByComparingTo("50.00000000");
    }

    @Test
    void emaReactsFasterThanSmaToARecentPriceJump() {
        // Flat at 100 for a while, then a jump to 120 for the last few points.
        List<BigDecimal> series = prices(100, 100, 100, 100, 100, 100, 100, 100, 120, 120, 120);
        BigDecimal sma = TechnicalIndicators.sma(series, 5);
        BigDecimal ema = TechnicalIndicators.ema(series, 5);

        // Both should have moved up from 100, but EMA weights recent (120) prices more
        // heavily than SMA's flat average over the same window.
        assertThat(ema).isGreaterThan(sma);
    }

    @Test
    void rsiIsOneHundredWhenThereAreOnlyGains() {
        List<BigDecimal> risingOnly = prices(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
        assertThat(TechnicalIndicators.rsi(risingOnly, 14)).isEqualByComparingTo("100.00000000");
    }

    @Test
    void rsiIsBetweenZeroAndOneHundredForMixedMovement() {
        List<BigDecimal> mixed = prices(
            44, 44.34, 44.09, 44.15, 43.61, 44.33, 44.83, 45.10, 45.42, 45.84,
            46.08, 45.89, 46.03, 45.61, 46.28
        );
        BigDecimal rsi = TechnicalIndicators.rsi(mixed, 14);
        assertThat(rsi).isGreaterThan(BigDecimal.ZERO);
        assertThat(rsi).isLessThanOrEqualTo(new BigDecimal("100"));
        // This series is mostly rising -> RSI should read as "overbought-leaning" (> 50).
        assertThat(rsi).isGreaterThan(new BigDecimal("50"));
    }

    @Test
    void rsiThrowsWhenNotEnoughData() {
        assertThatThrownBy(() -> TechnicalIndicators.rsi(prices(1, 2, 3), 14))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
