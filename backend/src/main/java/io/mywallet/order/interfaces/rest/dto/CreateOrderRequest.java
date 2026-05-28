package io.mywallet.order.interfaces.rest.dto;

import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.domain.model.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull UUID portfolioId,
    @NotNull UUID assetId,
    @NotNull OrderType orderType,
    @NotNull OrderSide side,
    @NotNull @DecimalMin(value = "0.00000001", message = "Quantity must be positive") BigDecimal quantity,
    BigDecimal limitPrice // required for LIMIT/STOP/... orders, null for MARKET - validated in the service
) {}
