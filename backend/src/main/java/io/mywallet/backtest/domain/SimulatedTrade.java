package io.mywallet.backtest.domain;

import java.math.BigDecimal;

public record SimulatedTrade(
    int tickIndex,
    TradeSide side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fee
) {
    public enum TradeSide { BUY, SELL }
}
