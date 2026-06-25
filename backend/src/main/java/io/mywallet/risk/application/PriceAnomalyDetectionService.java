package io.mywallet.risk.application;

import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.risk.domain.detection.StreamingZScore;
import io.mywallet.risk.infrastructure.persistence.MarketAnomalyEntity;
import io.mywallet.risk.infrastructure.persistence.MarketAnomalyJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One {@link StreamingZScore} kept in memory per asset, updated on every simulated price
 * tick from {@code MarketDataGeneratorService}. O(1) per tick regardless of how long the
 * process has been running - this is exactly why the streaming (Welford's algorithm)
 * formulation was chosen over recomputing statistics from stored history on every tick.
 *
 * <p>A tick is flagged anomalous if its z-score (computed against the statistics of every
 * <em>prior</em> tick, not including itself) exceeds the configured threshold in either
 * direction. Given the market data itself follows GBM (see {@code GbmPriceModel}), a truly
 * wild single-tick jump is rare by construction - which is exactly what makes this a
 * reasonable, low-noise detector for the demo: false positives should be uncommon.</p>
 */
@Service
public class PriceAnomalyDetectionService {

    private final Map<UUID, StreamingZScore> statsByAsset = new ConcurrentHashMap<>();
    private final MarketAnomalyJpaRepository anomalyRepository;
    private final WebSocketBroadcaster broadcaster;
    private final double zScoreThreshold;

    public PriceAnomalyDetectionService(
        MarketAnomalyJpaRepository anomalyRepository,
        WebSocketBroadcaster broadcaster,
        @Value("${mywallet.detection.price-anomaly.z-score-threshold:4.0}") double zScoreThreshold
    ) {
        this.anomalyRepository = anomalyRepository;
        this.broadcaster = broadcaster;
        this.zScoreThreshold = zScoreThreshold;
    }

    /** Called once per asset per tick, right after the new price is computed and persisted. */
    public void observe(UUID assetId, String symbol, BigDecimal newPrice) {
        StreamingZScore stats = statsByAsset.getOrDefault(assetId, StreamingZScore.empty());

        double zScore = stats.zScoreOf(newPrice);
        if (Math.abs(zScore) >= zScoreThreshold) {
            recordAnomaly(assetId, symbol, newPrice, zScore, stats);
        }

        statsByAsset.put(assetId, stats.update(newPrice));
    }

    private void recordAnomaly(UUID assetId, String symbol, BigDecimal price, double zScore, StreamingZScore priorStats) {
        BigDecimal roundedZ = BigDecimal.valueOf(zScore).setScale(2, RoundingMode.HALF_UP);
        String explanation = "%s price %s is %s standard deviations from the running mean %s (based on %d prior ticks)"
            .formatted(symbol, price, roundedZ, priorStats.mean(), priorStats.count());

        anomalyRepository.save(new MarketAnomalyEntity(UUID.randomUUID(), assetId, price, roundedZ, explanation));
        broadcaster.broadcastMarketAnomaly(symbol, price, roundedZ, explanation);
    }
}
