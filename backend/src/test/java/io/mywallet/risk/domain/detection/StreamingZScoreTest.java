package io.mywallet.risk.domain.detection;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StreamingZScoreTest {

    @Test
    void zScoreIsZeroWithFewerThanTwoObservations() {
        var stats = StreamingZScore.empty();
        assertThat(stats.zScoreOf(new BigDecimal("999"))).isZero();

        stats = stats.update(new BigDecimal("10"));
        assertThat(stats.zScoreOf(new BigDecimal("999"))).isZero(); // still only 1 observation
    }

    @Test
    void matchesHandComputedZScoreForASimpleSequence() {
        // [1, 2, 3] -> mean = 2, sample stdev = sqrt(((1-2)^2+(2-2)^2+(3-2)^2)/(3-1)) = sqrt(1) = 1
        var stats = StreamingZScore.empty()
            .update(new BigDecimal("1"))
            .update(new BigDecimal("2"))
            .update(new BigDecimal("3"));

        assertThat(stats.mean().doubleValue()).isCloseTo(2.0, within(1e-9));
        assertThat(stats.zScoreOf(new BigDecimal("4"))).isCloseTo(2.0, within(1e-9)); // (4-2)/1
        assertThat(stats.zScoreOf(new BigDecimal("0"))).isCloseTo(-2.0, within(1e-9)); // (0-2)/1
        assertThat(stats.zScoreOf(new BigDecimal("2"))).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void flagsAClearOutlierWithAHighZScore() {
        var stats = StreamingZScore.empty()
            .update(new BigDecimal("10"))
            .update(new BigDecimal("12"))
            .update(new BigDecimal("11"))
            .update(new BigDecimal("13"));

        double zScore = stats.zScoreOf(new BigDecimal("100"));
        assertThat(Math.abs(zScore)).isGreaterThan(10.0); // wildly outside normal range
    }

    @Test
    void countIncrementsWithEachUpdate() {
        var stats = StreamingZScore.empty().update(BigDecimal.ONE).update(BigDecimal.TEN);
        assertThat(stats.count()).isEqualTo(2);
    }

    @Test
    void isImmutableAcrossUpdates() {
        var original = StreamingZScore.empty().update(new BigDecimal("5"));
        var updated = original.update(new BigDecimal("10"));

        assertThat(original.count()).isEqualTo(1); // unaffected by the update() call above
        assertThat(updated.count()).isEqualTo(2);
    }

    @Test
    void zeroVarianceReturnsZeroRatherThanDividingByZero() {
        var stats = StreamingZScore.empty().update(new BigDecimal("5")).update(new BigDecimal("5")).update(new BigDecimal("5"));
        assertThat(stats.zScoreOf(new BigDecimal("999"))).isZero();
    }
}
