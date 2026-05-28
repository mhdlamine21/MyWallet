package io.mywallet.portfolio.interfaces.rest.dto;

import io.mywallet.portfolio.infrastructure.persistence.PortfolioPositionEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record PositionResponse(
    UUID id,
    UUID assetId,
    BigDecimal quantity,
    BigDecimal averageAcquisitionPrice
) {
    public static PositionResponse from(PortfolioPositionEntity entity) {
        return new PositionResponse(
            entity.getId(),
            entity.getAssetId(),
            entity.getQuantity(),
            entity.getAverageAcquisitionPrice()
        );
    }
}
