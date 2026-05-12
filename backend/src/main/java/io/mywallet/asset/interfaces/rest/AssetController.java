package io.mywallet.asset.interfaces.rest;

import io.mywallet.asset.infrastructure.persistence.AssetJpaRepository;
import io.mywallet.asset.interfaces.rest.dto.AssetResponse;
import io.mywallet.marketdata.infrastructure.persistence.MarketPriceJpaRepository;
import io.mywallet.marketdata.interfaces.rest.dto.MarketPriceResponse;
import io.mywallet.risk.infrastructure.persistence.MarketAnomalyEntity;
import io.mywallet.risk.infrastructure.persistence.MarketAnomalyJpaRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets")
public class AssetController {

    private final AssetJpaRepository assetRepository;
    private final MarketPriceJpaRepository marketPriceRepository;
    private final MarketAnomalyJpaRepository anomalyRepository;

    public AssetController(
        AssetJpaRepository assetRepository,
        MarketPriceJpaRepository marketPriceRepository,
        MarketAnomalyJpaRepository anomalyRepository
    ) {
        this.assetRepository = assetRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.anomalyRepository = anomalyRepository;
    }

    @GetMapping
    public List<AssetResponse> list() {
        return assetRepository.findByEnabledTrue().stream().map(AssetResponse::from).toList();
    }

    @GetMapping("/{symbol}/prices")
    public List<MarketPriceResponse> prices(@PathVariable String symbol,
                                             @RequestParam(defaultValue = "200") int limit) {
        var asset = assetRepository.findBySymbol(symbol)
            .orElseThrow(() -> new NoSuchElementException("Unknown asset symbol: " + symbol));
        int bounded = Math.min(Math.max(limit, 1), 2000);

        // Fetched most-recent-first, then reversed so the response is chronological
        // (oldest first) - what a chart or backtest client actually wants to plot.
        List<MarketPriceResponse> recent = marketPriceRepository.findByAssetIdOrderByObservedAtDesc(asset.getId(), Limit.of(bounded))
            .stream().map(MarketPriceResponse::from).toList();
        return recent.reversed();
    }

    @GetMapping("/{symbol}/anomalies")
    public List<AnomalyResponse> anomalies(@PathVariable String symbol) {
        var asset = assetRepository.findBySymbol(symbol)
            .orElseThrow(() -> new NoSuchElementException("Unknown asset symbol: " + symbol));
        return anomalyRepository.findByAssetIdOrderByDetectedAtDesc(asset.getId()).stream()
            .map(AnomalyResponse::from)
            .toList();
    }

    public record AnomalyResponse(UUID id, BigDecimal price, BigDecimal zScore, String explanation, Instant detectedAt) {
        static AnomalyResponse from(MarketAnomalyEntity entity) {
            return new AnomalyResponse(entity.getId(), entity.getPrice(), entity.getZScore(), entity.getExplanation(), entity.getDetectedAt());
        }
    }
}
