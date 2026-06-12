package io.mywallet.marketdata.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_prices")
public class MarketPriceEntity {

    @Id
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    protected MarketPriceEntity() {
        // JPA
    }

    public MarketPriceEntity(UUID id, UUID assetId, BigDecimal price, Instant observedAt) {
        this.id = id;
        this.assetId = assetId;
        this.price = price;
        this.observedAt = observedAt;
    }

    public UUID getId() { return id; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getPrice() { return price; }
    public Instant getObservedAt() { return observedAt; }
}
