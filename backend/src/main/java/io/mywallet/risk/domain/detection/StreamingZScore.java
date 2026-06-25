package io.mywallet.risk.domain.detection;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Online (streaming) mean and variance via Welford's algorithm - the whole point is
 * <strong>not</strong> needing to keep every past price in memory to know whether the
 * current one is anomalous: each new observation updates the running mean/variance in
 * O(1) time and O(1) space, however many ticks have already been seen. This is what makes
 * it usable directly inside {@code MarketDataGeneratorService}'s per-tick loop for every
 * asset, continuously, without the memory footprint growing over the life of the process.
 *
 * <p>Immutable-update style: {@link #update(BigDecimal)} returns a new instance rather
 * than mutating in place, so a caller holding a reference to a prior state (e.g. for
 * logging "what it was before this tick") isn't surprised by it changing underneath them.
 * The per-asset caller (see {@code PriceAnomalyDetectionService}) is expected to just
 * reassign its stored instance to the returned one.</p>
 */
public final class StreamingZScore {

    private final long count;
    private final double mean;
    private final double sumSquaredDeviations; // Welford's M2

    private StreamingZScore(long count, double mean, double sumSquaredDeviations) {
        this.count = count;
        this.mean = mean;
        this.sumSquaredDeviations = sumSquaredDeviations;
    }

    public static StreamingZScore empty() {
        return new StreamingZScore(0, 0.0, 0.0);
    }

    /** Folds one new observation into the running statistics, returning the updated state. */
    public StreamingZScore update(BigDecimal value) {
        double x = value.doubleValue();
        long newCount = count + 1;
        double delta = x - mean;
        double newMean = mean + delta / newCount;
        double delta2 = x - newMean;
        double newM2 = sumSquaredDeviations + delta * delta2;
        return new StreamingZScore(newCount, newMean, newM2);
    }

    /**
     * How many standard deviations {@code value} is from the mean observed <em>so far</em>
     * (i.e. before folding {@code value} itself in - this answers "was this new tick
     * surprising given everything before it", which is the actual anomaly-detection
     * question, not "how far is it from a mean that already includes it").
     *
     * @return 0 if fewer than 2 observations have been seen yet (no variance to divide by)
     */
    public double zScoreOf(BigDecimal value) {
        if (count < 2) {
            return 0.0;
        }
        double stdDev = Math.sqrt(sumSquaredDeviations / (count - 1)); // sample stdev
        if (stdDev == 0) {
            return 0.0;
        }
        return (value.doubleValue() - mean) / stdDev;
    }

    public long count() {
        return count;
    }

    public BigDecimal mean() {
        return BigDecimal.valueOf(mean).setScale(8, RoundingMode.HALF_UP);
    }
}
