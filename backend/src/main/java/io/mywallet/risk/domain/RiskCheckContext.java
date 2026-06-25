package io.mywallet.risk.domain;

import io.mywallet.order.domain.model.OrderSide;

import java.math.BigDecimal;

/**
 * Everything a {@link RiskCheck} needs to evaluate one order, assembled by the
 * application layer (which does the real DB reads) before any check runs. Checks
 * themselves are pure functions over this context - no I/O, trivially unit-testable,
 * consistent with {@code GbmPriceModel} and the rule engine's evaluator.
 */
public record RiskCheckContext(
    OrderSide orderSide,
    BigDecimal orderQuantity,
    BigDecimal orderValue,               // orderQuantity * reference price
    BigDecimal availableCashBalance,
    BigDecimal existingPositionQuantity, // for the asset being traded, 0 if none held
    BigDecimal existingPositionValue,    // current market value of that position, 0 if none held
    BigDecimal totalPortfolioValue,      // cash + sum(all positions at current market price)
    int ordersPlacedToday,
    RiskLimits limits
) {
}
