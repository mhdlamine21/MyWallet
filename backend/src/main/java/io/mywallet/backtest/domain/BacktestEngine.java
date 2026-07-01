package io.mywallet.backtest.domain;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.evaluator.RuleEvaluator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a single-asset strategy over a historical price series: at every tick, the
 * rule is re-evaluated; a transition from false->true triggers a simulated all-in BUY, a
 * transition from true->false triggers a full SELL (liquidation). This binary "signal ->
 * be long or be flat" model is a deliberate simplification - no partial position sizing,
 * no pyramiding, no short-selling - chosen because it's what the rule engine's boolean
 * output directly and unambiguously maps to, and multi-tier position sizing would need
 * risk-engine integration (Phase 7b: VaR/Kelly) this phase doesn't yet wire in.
 *
 * <p>During the warm-up period (before enough history exists for whatever indicator
 * periods the rule uses), the rule can't be evaluated - the engine simply stays flat
 * rather than erroring, exactly like a real strategy would before it has enough data.</p>
 */
public final class BacktestEngine {

    private BacktestEngine() {
    }

    public record Config(
        BigDecimal initialCapital,
        BigDecimal feeRate,       // fraction per trade, e.g. 0.001 = 0.1%
        BigDecimal slippageRate,  // fraction applied against the trader, e.g. 0.0005
        double periodsPerYear,
        BigDecimal riskFreeRatePerPeriod
    ) {
    }

    public static BacktestOutcome run(List<BigDecimal> prices, BooleanExpression rule, Config config) {
        if (prices.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 price points to run a backtest, got " + prices.size());
        }

        BigDecimal cash = config.initialCapital();
        BigDecimal positionQty = BigDecimal.ZERO;
        List<BigDecimal> equityCurve = new ArrayList<>(prices.size());
        List<BigDecimal> buyAndHoldCurve = new ArrayList<>(prices.size());
        List<SimulatedTrade> trades = new ArrayList<>();

        BigDecimal buyAndHoldQty = config.initialCapital().divide(prices.get(0), 8, RoundingMode.HALF_UP);

        for (int i = 0; i < prices.size(); i++) {
            BigDecimal currentPrice = prices.get(i);
            List<BigDecimal> historyUpToNow = prices.subList(0, i + 1);

            BigDecimal equityBeforeDecision = cash.add(positionQty.multiply(currentPrice));
            BigDecimal exposure = equityBeforeDecision.signum() == 0
                ? BigDecimal.ZERO
                : positionQty.multiply(currentPrice).divide(equityBeforeDecision, 8, RoundingMode.HALF_UP);

            boolean signal = tryEvaluateSignal(rule, historyUpToNow, exposure);
            boolean isLong = positionQty.signum() > 0;

            if (signal && !isLong) {
                BigDecimal executionPrice = currentPrice.multiply(BigDecimal.ONE.add(config.slippageRate()));
                BigDecimal fee = cash.multiply(config.feeRate());
                BigDecimal investable = cash.subtract(fee);
                if (investable.signum() > 0) {
                    BigDecimal qty = investable.divide(executionPrice, 8, RoundingMode.DOWN);
                    if (qty.signum() > 0) {
                        cash = cash.subtract(qty.multiply(executionPrice)).subtract(fee);
                        positionQty = qty;
                        trades.add(new SimulatedTrade(i, SimulatedTrade.TradeSide.BUY, qty, executionPrice, fee));
                    }
                }
            } else if (!signal && isLong) {
                BigDecimal executionPrice = currentPrice.multiply(BigDecimal.ONE.subtract(config.slippageRate()));
                BigDecimal proceeds = positionQty.multiply(executionPrice);
                BigDecimal fee = proceeds.multiply(config.feeRate());
                cash = cash.add(proceeds).subtract(fee);
                trades.add(new SimulatedTrade(i, SimulatedTrade.TradeSide.SELL, positionQty, executionPrice, fee));
                positionQty = BigDecimal.ZERO;
            }

            equityCurve.add(cash.add(positionQty.multiply(currentPrice)));
            buyAndHoldCurve.add(buyAndHoldQty.multiply(currentPrice));
        }

        // Liquidate any still-open position so "final capital" is a clean, comparable cash figure.
        if (positionQty.signum() > 0) {
            int lastIndex = prices.size() - 1;
            BigDecimal lastPrice = prices.get(lastIndex);
            BigDecimal executionPrice = lastPrice.multiply(BigDecimal.ONE.subtract(config.slippageRate()));
            BigDecimal proceeds = positionQty.multiply(executionPrice);
            BigDecimal fee = proceeds.multiply(config.feeRate());
            cash = cash.add(proceeds).subtract(fee);
            trades.add(new SimulatedTrade(lastIndex, SimulatedTrade.TradeSide.SELL, positionQty, executionPrice, fee));
            equityCurve.set(lastIndex, cash);
        }

        return buildOutcome(cash, equityCurve, buyAndHoldCurve, trades, config);
    }

    private static boolean tryEvaluateSignal(BooleanExpression rule, List<BigDecimal> historyUpToNow, BigDecimal exposure) {
        try {
            var context = new BacktestMarketContext(historyUpToNow, exposure);
            return new RuleEvaluator(context).evaluate(rule);
        } catch (IllegalArgumentException notEnoughHistoryYet) {
            return false; // still warming up - stay flat
        }
    }

    private static BacktestOutcome buildOutcome(
        BigDecimal finalCapital, List<BigDecimal> equityCurve, List<BigDecimal> buyAndHoldCurve,
        List<SimulatedTrade> trades, Config config
    ) {
        List<BigDecimal> returns = PerformanceMetrics.periodReturns(equityCurve);
        BigDecimal totalReturn = PerformanceMetrics.totalReturn(config.initialCapital(), finalCapital);

        List<BigDecimal> roundTripPnls = roundTripPnls(trades);
        long winningTrades = roundTripPnls.stream().filter(p -> p.signum() > 0).count();
        BigDecimal winRate = roundTripPnls.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(roundTripPnls.size()), 4, RoundingMode.HALF_UP);

        BigDecimal averageGain = average(roundTripPnls.stream().filter(p -> p.signum() > 0).toList());
        BigDecimal averageLoss = average(roundTripPnls.stream().filter(p -> p.signum() < 0).toList());

        return new BacktestOutcome(
            finalCapital,
            totalReturn,
            PerformanceMetrics.annualizedReturn(totalReturn, equityCurve.size(), config.periodsPerYear()),
            roundTripPnls.size(),
            winRate,
            averageGain,
            averageLoss,
            PerformanceMetrics.maxDrawdown(equityCurve),
            PerformanceMetrics.sharpeRatio(returns, config.riskFreeRatePerPeriod(), config.periodsPerYear()),
            PerformanceMetrics.sortinoRatio(returns, config.riskFreeRatePerPeriod(), config.periodsPerYear()),
            equityCurve,
            buyAndHoldCurve,
            trades
        );
    }

    private static List<BigDecimal> roundTripPnls(List<SimulatedTrade> trades) {
        List<BigDecimal> pnls = new ArrayList<>();
        for (int i = 0; i + 1 < trades.size(); i += 2) {
            SimulatedTrade buy = trades.get(i);
            SimulatedTrade sell = trades.get(i + 1);
            BigDecimal cost = buy.quantity().multiply(buy.price()).add(buy.fee());
            BigDecimal proceeds = sell.quantity().multiply(sell.price()).subtract(sell.fee());
            pnls.add(proceeds.subtract(cost));
        }
        return pnls;
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }
}
