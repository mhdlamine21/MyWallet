package io.mywallet.portfolio.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_positions")
public class PortfolioPositionEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "average_acquisition_price", nullable = false)
    private BigDecimal averageAcquisitionPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PortfolioPositionEntity() {
        // JPA
    }

    public PortfolioPositionEntity(UUID id, UUID portfolioId, UUID assetId,
                                    BigDecimal quantity, BigDecimal averageAcquisitionPrice) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.quantity = quantity;
        this.averageAcquisitionPrice = averageAcquisitionPrice;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getAverageAcquisitionPrice() { return averageAcquisitionPrice; }
}
