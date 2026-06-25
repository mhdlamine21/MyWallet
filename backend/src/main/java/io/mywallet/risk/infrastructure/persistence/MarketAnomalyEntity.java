package io.mywallet.risk.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_anomalies")
public class MarketAnomalyEntity {

    @Id
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "z_score", nullable = false)
    private BigDecimal zScore;

    @Column(nullable = false)
    private String explanation;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    protected MarketAnomalyEntity() {
        // JPA
    }

    public MarketAnomalyEntity(UUID id, UUID assetId, BigDecimal price, BigDecimal zScore, String explanation) {
        this.id = id;
        this.assetId = assetId;
        this.price = price;
        this.zScore = zScore;
        this.explanation = explanation;
        this.detectedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getZScore() { return zScore; }
    public String getExplanation() { return explanation; }
    public Instant getDetectedAt() { return detectedAt; }
}
