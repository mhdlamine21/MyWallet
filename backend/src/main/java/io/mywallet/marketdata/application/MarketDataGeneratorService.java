package io.mywallet.marketdata.application;

import io.mywallet.asset.infrastructure.persistence.AssetEntity;
import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.infrastructure.websocket.WebSocketBroadcaster;
import io.mywallet.marketdata.domain.GbmPriceModel;
import io.mywallet.risk.application.PriceAnomalyDetectionService;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the simulated market: once per tick, computes a new GBM price step for every
 * enabled asset and persists it. Per-asset {@link Random} instances are seeded from
 * {@code AssetEntity.randomSeed} and kept alive for the process lifetime, so restarting
 * the app resets the sequence deterministically from the last persisted price rather than
 * replaying from asset creation - good enough for a demo; a future enhancement could
 * persist the RNG's internal state for exact resumability if that ever matters.
 *
 * <p>Hook point for later phases: this is where a Phase 5 WebSocket broadcast and a
 * Phase 9 "market shock injection" admin action would plug in - both just need to publish
 * the new price after it's computed here, without changing the GBM math itself.</p>
 */
@Service
public class MarketDataGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataGeneratorService.class);
    private static final double SECONDS_PER_YEAR = 365.0 * 24 * 3600;

    private final AssetJpaRepository assetRepository;
    private final MarketPriceJpaRepository priceRepository;
    private final WebSocketBroadcaster broadcaster;
    private final PriceAnomalyDetectionService anomalyDetectionService;
    private final long tickIntervalSeconds;

    private final Map<UUID, org.apache.commons.math3.random.MersenneTwister> randomByAsset = new ConcurrentHashMap<>();
    private final Map<UUID, BigDecimal> lastPriceByAsset = new ConcurrentHashMap<>();

    public MarketDataGeneratorService(
        AssetJpaRepository assetRepository,
        MarketPriceJpaRepository priceRepository,
        WebSocketBroadcaster broadcaster,
        PriceAnomalyDetectionService anomalyDetectionService,
        @Value("${mywallet.marketdata.tick-interval-seconds:5}") long tickIntervalSeconds
    ) {
        this.assetRepository = assetRepository;
        this.priceRepository = priceRepository;
        this.broadcaster = broadcaster;
        this.anomalyDetectionService = anomalyDetectionService;
        this.tickIntervalSeconds = tickIntervalSeconds;
    }

    @Scheduled(fixedDelayString = "${mywallet.marketdata.tick-interval-ms:5000}")
    @Transactional
    public void tick() {
        List<AssetEntity> assets = assetRepository.findByEnabledTrue();
        Instant now = Instant.now();
        double dtYears = tickIntervalSeconds / SECONDS_PER_YEAR;

        for (AssetEntity asset : assets) {
            try {
                BigDecimal newPrice = computeNextPrice(asset, dtYears);
                priceRepository.save(new MarketPriceEntity(UUID.randomUUID(), asset.getId(), newPrice, now));
                lastPriceByAsset.put(asset.getId(), newPrice);
                broadcaster.broadcastPrice(asset.getSymbol(), newPrice, now);
                anomalyDetectionService.observe(asset.getId(), asset.getSymbol(), newPrice);
            } catch (Exception e) {
                // One asset's price computation failing must not stop the tick for the
                // others - log and continue rather than letting @Scheduled's single
                // exception abort the whole batch.
                log.error("Failed to generate price tick for asset {}", asset.getSymbol(), e);
            }
        }
    }

    private BigDecimal computeNextPrice(AssetEntity asset, double dtYears) {
        org.apache.commons.math3.random.MersenneTwister random = 
            randomByAsset.computeIfAbsent(asset.getId(), id -> new org.apache.commons.math3.random.MersenneTwister(asset.getRandomSeed()));
        BigDecimal current = lastPriceByAsset.computeIfAbsent(asset.getId(), id ->
            priceRepository.findFirstByAssetIdOrderByObservedAtDesc(id)
                .map(MarketPriceEntity::getPrice)
                .orElse(asset.getInitialPrice()));

        return GbmPriceModel.nextPrice(current, asset.getDrift(), asset.getVolatility(), dtYears, random.nextGaussian());
    }
}
