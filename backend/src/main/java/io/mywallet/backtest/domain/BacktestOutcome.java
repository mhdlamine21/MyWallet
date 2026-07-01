package io.mywallet.backtest.domain;

import java.math.BigDecimal;
import java.util.List;

public record BacktestOutcome(
    BigDecimal finalCapital,
    BigDecimal totalReturn,
    BigDecimal annualizedReturn,
    int numberOfTrades,
    BigDecimal winRate,
    BigDecimal averageGain,
    BigDecimal averageLoss,
    BigDecimal maxDrawdown,
    BigDecimal sharpeRatio,
    BigDecimal sortinoRatio,
    List<BigDecimal> equityCurve,
    List<BigDecimal> buyAndHoldEquityCurve,
    List<SimulatedTrade> trades
) {
}
