package io.mywallet.asset.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An asset also carries its own GBM simulation parameters (drift, volatility, seed) -
 * see {@code MarketDataGeneratorService}. Real market data platforms wouldn't do this
 * (price generation and asset metadata would be separate concerns), but here the asset
 * *is* the configuration for its own simulated price process, which keeps things simple
 * for a project of this scope.
 */
@Entity
@Table(name = "assets")
public class AssetEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false)
    private AssetClass assetClass;

    @Column(nullable = false)
    private String currency;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "initial_price", nullable = false)
    private BigDecimal initialPrice;

    @Column(nullable = false)
    private BigDecimal drift;

    @Column(nullable = false)
    private BigDecimal volatility;

    @Column(name = "random_seed", nullable = false)
    private long randomSeed;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AssetEntity() {
        // JPA
    }

    public AssetEntity(UUID id, String symbol, AssetClass assetClass, String currency, String displayName,
                        BigDecimal initialPrice, BigDecimal drift, BigDecimal volatility, long randomSeed) {
        this.id = id;
        this.symbol = symbol;
        this.assetClass = assetClass;
        this.currency = currency;
        this.displayName = displayName;
        this.initialPrice = initialPrice;
        this.drift = drift;
        this.volatility = volatility;
        this.randomSeed = randomSeed;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public AssetClass getAssetClass() { return assetClass; }
    public String getCurrency() { return currency; }
    public String getDisplayName() { return displayName; }
    public BigDecimal getInitialPrice() { return initialPrice; }
    public BigDecimal getDrift() { return drift; }
    public BigDecimal getVolatility() { return volatility; }
    public long getRandomSeed() { return randomSeed; }
    public boolean isEnabled() { return enabled; }

    public enum AssetClass {
        STOCK, CRYPTO, CURRENCY, BOND, FUND, COMMODITY
    }
}
