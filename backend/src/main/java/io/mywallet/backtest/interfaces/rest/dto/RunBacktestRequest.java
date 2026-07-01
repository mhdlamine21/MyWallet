package io.mywallet.backtest.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RunBacktestRequest(
    @NotNull UUID assetId,
    @NotNull @DecimalMin("0.01") BigDecimal initialCapital,
    @NotNull Instant periodStart,
    @NotNull Instant periodEnd,
    BigDecimal feeRate,       // defaults to 0 if omitted
    BigDecimal slippageRate,  // defaults to 0 if omitted
    Long seed                 // defaults to a random seed if omitted - pass one for reproducible results
) {}
