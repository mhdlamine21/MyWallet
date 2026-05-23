package io.mywallet.order.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read projection of the Order aggregate - NOT the source of truth (that's
 * {@code domain_events}). Kept in sync by {@code OrderProjector}. Exists so that
 * "list my orders" / "filter by status" queries don't replay event history per row.
 */
@Entity
@Table(name = "order_projections")
public class OrderProjectionEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(nullable = false)
    private String side;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "limit_price")
    private BigDecimal limitPrice;

    @Column(name = "filled_quantity", nullable = false)
    private BigDecimal filledQuantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderProjectionEntity() {
        // JPA
    }

    public OrderProjectionEntity(UUID id, UUID portfolioId, UUID assetId, String orderType, String side,
                                  BigDecimal quantity, BigDecimal limitPrice, BigDecimal filledQuantity,
                                  String status, long version, Instant createdAt) {
        this.id = id;
        this.portfolioId = portfolioId;
        this.assetId = assetId;
        this.orderType = orderType;
        this.side = side;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
        this.filledQuantity = filledQuantity;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void applyFill(BigDecimal newFilledQuantity, String newStatus, long newVersion) {
        this.filledQuantity = newFilledQuantity;
        this.status = newStatus;
        this.version = newVersion;
        this.updatedAt = Instant.now();
    }

    public void applyCancellation(long newVersion) {
        this.status = "CANCELLED";
        this.version = newVersion;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPortfolioId() { return portfolioId; }
    public UUID getAssetId() { return assetId; }
    public String getOrderType() { return orderType; }
    public String getSide() { return side; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getLimitPrice() { return limitPrice; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
