package io.mywallet.order.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SimulateExecutionRequest(
    @NotNull @DecimalMin(value = "0.00000001", message = "Quantity must be positive") BigDecimal quantity,
    @NotNull @DecimalMin(value = "0.00000001", message = "Execution price must be positive") BigDecimal executionPrice,
    @NotNull @DecimalMin(value = "0.0", message = "Fees cannot be negative") BigDecimal fees,
    @NotBlank String externalReference
) {}
