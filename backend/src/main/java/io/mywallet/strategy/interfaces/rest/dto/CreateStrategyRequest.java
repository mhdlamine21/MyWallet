package io.mywallet.strategy.interfaces.rest.dto;

import io.mywallet.strategy.domain.RiskLevel;
import io.mywallet.strategy.domain.StrategyMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStrategyRequest(
    @NotNull UUID portfolioId,
    @NotBlank String name,
    String description,
    @NotNull StrategyMode mode,
    RiskLevel riskLevel,
    BigDecimal maximumCapital,
    BigDecimal maximumLoss,
    @NotBlank String ruleExpression
) {}
