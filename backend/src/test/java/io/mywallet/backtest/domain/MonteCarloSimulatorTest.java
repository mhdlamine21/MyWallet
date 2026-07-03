package io.mywallet.backtest.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MonteCarloSimulatorTest {

    @Test
    void allBandsStartAtInitialCapital() {
        var returns = List.of(new BigDecimal("0.01"), new BigDecimal("-0.02"), new BigDecimal("0.03"));
        var band = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 10, 500, 42L);

        assertThat(band.p5().get(0)).isEqualByComparingTo("1000.00");
        assertThat(band.p50().get(0)).isEqualByComparingTo("1000.00");
        assertThat(band.p95().get(0)).isEqualByComparingTo("1000.00");
    }

    @Test
    void bandsAreOrderedP5LessOrEqualP50LessOrEqualP95AtEveryTick() {
        var returns = List.of(new BigDecimal("0.02"), new BigDecimal("-0.03"), new BigDecimal("0.05"), new BigDecimal("-0.01"));
        var band = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 20, 1000, 7L);

        for (int t = 0; t < band.p50().size(); t++) {
            assertThat(band.p5().get(t)).isLessThanOrEqualTo(band.p50().get(t));
            assertThat(band.p50().get(t)).isLessThanOrEqualTo(band.p95().get(t));
        }
    }

    @Test
    void sameSeedProducesIdenticalResults() {
        var returns = List.of(new BigDecimal("0.01"), new BigDecimal("-0.02"));
        var bandA = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 15, 200, 123L);
        var bandB = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 15, 200, 123L);

        assertThat(bandA.p50()).isEqualTo(bandB.p50());
        assertThat(bandA.p5()).isEqualTo(bandB.p5());
        assertThat(bandA.p95()).isEqualTo(bandB.p95());
    }

    @Test
    void differentSeedsCanProduceDifferentResults() {
        var returns = List.of(new BigDecimal("0.05"), new BigDecimal("-0.05"), new BigDecimal("0.10"), new BigDecimal("-0.10"));
        var bandA = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 30, 200, 1L);
        var bandB = MonteCarloSimulator.simulate(new BigDecimal("1000"), returns, 30, 200, 2L);

        assertThat(bandA.p50()).isNotEqualTo(bandB.p50());
    }

    @Test
    void widerReturnDispersionProducesAWiderBandOverTime() {
        var tightReturns = List.of(new BigDecimal("0.01"), new BigDecimal("0.011"), new BigDecimal("0.009"));
        var wideReturns = List.of(new BigDecimal("0.20"), new BigDecimal("-0.20"), new BigDecimal("0.01"));

        var tightBand = MonteCarloSimulator.simulate(new BigDecimal("1000"), tightReturns, 20, 1000, 99L);
        var wideBand = MonteCarloSimulator.simulate(new BigDecimal("1000"), wideReturns, 20, 1000, 99L);

        double tightSpread = tightBand.p95().get(20).doubleValue() - tightBand.p5().get(20).doubleValue();
        double wideSpread = wideBand.p95().get(20).doubleValue() - wideBand.p5().get(20).doubleValue();

        assertThat(wideSpread).isGreaterThan(tightSpread);
    }

    @Test
    void emptyReturnsProducesAFlatBandAtInitialCapital() {
        var band = MonteCarloSimulator.simulate(new BigDecimal("1000"), List.of(), 10, 100, 1L);
        assertThat(band.p50()).containsExactly(new BigDecimal("1000"));
    }
}
