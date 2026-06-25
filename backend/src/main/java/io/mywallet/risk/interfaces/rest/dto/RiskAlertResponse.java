package io.mywallet.risk.interfaces.rest.dto;

import io.mywallet.risk.infrastructure.persistence.RiskAlertEntity;

import java.time.Instant;
import java.util.UUID;

public record RiskAlertResponse(
    UUID id,
    UUID portfolioId,
    String limitType,
    String level,
    int riskScore,
    String explanation,
    Instant raisedAt
) {
    public static RiskAlertResponse from(RiskAlertEntity entity) {
        return new RiskAlertResponse(
            entity.getId(), entity.getPortfolioId(), entity.getLimitType(),
            entity.getLevel(), entity.getRiskScore(), entity.getExplanation(), entity.getRaisedAt()
        );
    }
}
