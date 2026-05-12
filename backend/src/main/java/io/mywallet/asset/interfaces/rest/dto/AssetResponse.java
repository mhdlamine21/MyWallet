package io.mywallet.asset.interfaces.rest.dto;

import io.mywallet.asset.infrastructure.persistence.AssetEntity;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(
    UUID id,
    String symbol,
    String assetClass,
    String currency,
    String displayName,
    BigDecimal initialPrice
) {
    public static AssetResponse from(AssetEntity entity) {
        return new AssetResponse(
            entity.getId(), entity.getSymbol(), entity.getAssetClass().name(),
            entity.getCurrency(), entity.getDisplayName(), entity.getInitialPrice()
        );
    }
}
