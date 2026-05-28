package io.mywallet.portfolio.interfaces.rest.dto;

import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(
    UUID id,
    UUID accountId,
    String name,
    String mode,
    BigDecimal cashBalance,
    Instant createdAt,
    Instant updatedAt
) {
    public static PortfolioResponse from(PortfolioProjectionEntity entity) {
        return new PortfolioResponse(
            entity.getId(),
            entity.getAccountId(),
            entity.getName(),
            entity.getMode(),
            entity.getCashBalance(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
