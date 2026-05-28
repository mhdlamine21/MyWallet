package io.mywallet.portfolio.interfaces.rest.dto;

import io.mywallet.portfolio.domain.model.PortfolioMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePortfolioRequest(
    @NotNull UUID accountId,
    @NotBlank String name,
    @NotNull PortfolioMode mode,
    @NotNull @DecimalMin(value = "0.0", message = "Initial cash balance cannot be negative") BigDecimal initialCashBalance
) {}
