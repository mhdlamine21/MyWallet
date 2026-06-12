package io.mywallet.marketdata.interfaces.rest.dto;

import io.mywallet.marketdata.infrastructure.persistence.MarketPriceEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceResponse(BigDecimal price, Instant observedAt) {
    public static MarketPriceResponse from(MarketPriceEntity entity) {
        return new MarketPriceResponse(entity.getPrice(), entity.getObservedAt());
    }
}
