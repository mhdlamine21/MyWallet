package io.mywallet.strategy.interfaces.rest.dto;

import io.mywallet.strategy.infrastructure.persistence.StrategyEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StrategyResponse(
    UUID id,
    String name,
    String description,
    UUID portfolioId,
    String status,
    String mode,
    String riskLevel,
    BigDecimal maximumCapital,
    BigDecimal maximumLoss,
    String ruleExpression,
    Instant createdAt,
    Instant updatedAt
) {
    public static StrategyResponse from(StrategyEntity entity) {
        return new StrategyResponse(
            entity.getId(), entity.getName(), entity.getDescription(), entity.getPortfolioId(),
            entity.getStatus().name(), entity.getMode().name(), entity.getRiskLevel().name(),
            entity.getMaximumCapital(), entity.getMaximumLoss(), entity.getRuleExpression(),
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
