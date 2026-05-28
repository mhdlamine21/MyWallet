package io.mywallet.order.interfaces.rest.dto;

import io.mywallet.order.infrastructure.persistence.OrderProjectionEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID portfolioId,
    UUID assetId,
    String orderType,
    String side,
    BigDecimal quantity,
    BigDecimal limitPrice,
    BigDecimal filledQuantity,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(OrderProjectionEntity entity) {
        return new OrderResponse(
            entity.getId(), entity.getPortfolioId(), entity.getAssetId(), entity.getOrderType(),
            entity.getSide(), entity.getQuantity(), entity.getLimitPrice(), entity.getFilledQuantity(),
            entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
