package io.mywallet.ruleengine.domain.indicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure technical-indicator calculations over a chronologically ordered price series
 * (oldest first). No Spring, no I/O - these are exactly as testable as
 * {@code GbmPriceModel} and follow the same "compute in double, store/compare in
 * BigDecimal at the boundary" rule: these are statistical calculations, not money.
 *
 * <p>Phase 6 ships SMA, EMA, and RSI - the three needed by the reference example in the
 * project brief ("SMA(BTCUSDT, 20) &gt; SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) &lt; 70").
 * MACD, Bollinger Bands, ATR, VWAP, and OBV are documented extension points (see
 * {@code IndicatorType}) left for a Phase 6b follow-up rather than shipped shallow -
 * they need volume data the current market-data generator doesn't produce yet (VWAP,
 * OBV) or are meaningfully more involved to get right (MACD signal-line smoothing,
 * Bollinger band width).</p>
 */
public final class TechnicalIndicators {

    private TechnicalIndicators() {
    }

    /**
     * Converts a chronological list of prices to a ta4j BarSeries.
     */
    public static org.ta4j.core.BarSeries toBarSeries(List<BigDecimal> prices) {
        org.ta4j.core.BarSeries series = new org.ta4j.core.BaseBarSeries();
        java.time.ZonedDateTime start = java.time.ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, java.time.ZoneId.of("UTC"));
        for (int i = 0; i < prices.size(); i++) {
            BigDecimal p = prices.get(i);
            series.addBar(start.plusMinutes(i), p, p, p, p, BigDecimal.ONE);
        }
        return series;
    }

    /**
     * Simple Moving Average over the last {@code period} prices using ta4j's SMAIndicator.
     *
     * @throws IllegalArgumentException if fewer than {@code period} prices are available
     */
    public static BigDecimal sma(List<BigDecimal> prices, int period) {
        requireEnoughData(prices, period);
        org.ta4j.core.BarSeries series = toBarSeries(prices);
        org.ta4j.core.indicators.SMAIndicator indicator = new org.ta4j.core.indicators.SMAIndicator(
            new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series), period);
        return BigDecimal.valueOf(indicator.getValue(series.getEndIndex()).doubleValue())
            .setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Exponential Moving Average using ta4j's EMAIndicator.
     */
    public static BigDecimal ema(List<BigDecimal> prices, int period) {
        requireEnoughData(prices, period);
        org.ta4j.core.BarSeries series = toBarSeries(prices);
        org.ta4j.core.indicators.EMAIndicator indicator = new org.ta4j.core.indicators.EMAIndicator(
            new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series), period);
        return BigDecimal.valueOf(indicator.getValue(series.getEndIndex()).doubleValue())
            .setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Relative Strength Index using ta4j's RSIIndicator (Wilder's smoothing).
     * Returns a value in [0, 100].
     */
    public static BigDecimal rsi(List<BigDecimal> prices, int period) {
        requireEnoughData(prices, period + 1);
        org.ta4j.core.BarSeries series = toBarSeries(prices);
        org.ta4j.core.indicators.RSIIndicator indicator = new org.ta4j.core.indicators.RSIIndicator(
            new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series), period);
        return BigDecimal.valueOf(indicator.getValue(series.getEndIndex()).doubleValue())
            .setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * MACD (Moving Average Convergence Divergence) using ta4j's MACDIndicator.
     */
    public static BigDecimal macd(List<BigDecimal> prices, int shortPeriod, int longPeriod) {
        requireEnoughData(prices, longPeriod);
        org.ta4j.core.BarSeries series = toBarSeries(prices);
        org.ta4j.core.indicators.MACDIndicator indicator = new org.ta4j.core.indicators.MACDIndicator(
            new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series), shortPeriod, longPeriod);
        return BigDecimal.valueOf(indicator.getValue(series.getEndIndex()).doubleValue())
            .setScale(8, RoundingMode.HALF_UP);
    }

    private static void requireEnoughData(List<BigDecimal> prices, int required) {
        if (prices.size() < required) {
            throw new IllegalArgumentException(
                "Not enough price history: need at least %d points, got %d".formatted(required, prices.size()));
        }
    }
}
