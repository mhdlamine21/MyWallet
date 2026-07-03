package io.mywallet.backtest.domain;

import io.mywallet.ruleengine.domain.parser.RuleParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BacktestEngineTest {

    private static List<BigDecimal> prices(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    private BacktestEngine.Config config(BigDecimal capital, BigDecimal fee, BigDecimal slippage) {
        return new BacktestEngine.Config(capital, fee, slippage, 252, BigDecimal.ZERO);
    }

    @Test
    void buyAndHoldCurveTracksPriceExactlyRegardlessOfTheStrategy() {
        // Rule that's always false -> strategy stays flat the whole time, but the
        // buy-and-hold comparison curve must still track price 1:1 from the start.
        var rule = RuleParser.parse("SMA(BTCUSDT, 2) > 999999");
        var outcome = BacktestEngine.run(prices(100, 100, 110, 120), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO));

        // 1000 / 100 = 10 units bought at t0 (hypothetically) -> value at each tick = 10 * price
        assertThat(outcome.buyAndHoldEquityCurve().get(0).doubleValue()).isCloseTo(1000.0, within(0.01));
        assertThat(outcome.buyAndHoldEquityCurve().get(3).doubleValue()).isCloseTo(1200.0, within(0.01));
    }

    @Test
    void aRuleThatIsAlwaysTrueBuysImmediatelyAndNeverSells() {
        // "1 == 1" is always true from the very first tick.
        var rule = RuleParser.parse("1 == 1");
        var outcome = BacktestEngine.run(prices(100, 110, 121), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO));

        // Bought at t0 (100), never sold until forced liquidation at the end (121).
        // 1000 / 100 = 10 units -> final capital = 10 * 121 = 1210 (matches 10%+10% compounding).
        assertThat(outcome.finalCapital().doubleValue()).isCloseTo(1210.0, within(0.01));
        assertThat(outcome.totalReturn().doubleValue()).isCloseTo(0.21, within(0.001));
        assertThat(outcome.numberOfTrades()).isEqualTo(1); // one round trip: buy then forced final sell
    }

    @Test
    void feesAndSlippageReduceFinalCapitalComparedToFrictionlessExecution() {
        var rule = RuleParser.parse("1 == 1");
        var frictionless = BacktestEngine.run(prices(100, 110, 121), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO));
        var withFriction = BacktestEngine.run(prices(100, 110, 121), rule,
            config(new BigDecimal("1000"), new BigDecimal("0.01"), new BigDecimal("0.005")));

        assertThat(withFriction.finalCapital()).isLessThan(frictionless.finalCapital());
    }

    @Test
    void aRuleThatTogglesProducesAlternatingBuySellTrades() {
        // SMA(1) > SMA(2): with prices [10,10,10,20,10,10] this flips true right when
        // the price jumps (short-period average reacts immediately, longer one lags),
        // then flips back false once the average catches up.
        var rule = RuleParser.parse("SMA(X, 1) > SMA(X, 2)");
        var outcome = BacktestEngine.run(prices(10, 10, 10, 20, 10, 10), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(outcome.trades()).isNotEmpty();
        // Trades must strictly alternate BUY, SELL, BUY, SELL, ...
        for (int i = 0; i < outcome.trades().size(); i++) {
            var expectedSide = i % 2 == 0 ? SimulatedTrade.TradeSide.BUY : SimulatedTrade.TradeSide.SELL;
            assertThat(outcome.trades().get(i).side()).isEqualTo(expectedSide);
        }
    }

    @Test
    void rejectsTooShortAPriceSeries() {
        var rule = RuleParser.parse("1 == 1");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            BacktestEngine.run(prices(100), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stayFlatDuringWarmupRatherThanThrowing() {
        // RSI(14) needs 15 points; with only 5 points available the rule can't be
        // evaluated yet - the engine must stay flat, not blow up.
        var rule = RuleParser.parse("RSI(X, 14) > 50");
        var outcome = BacktestEngine.run(prices(10, 11, 12, 13, 14), rule, config(new BigDecimal("1000"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(outcome.trades()).isEmpty();
        assertThat(outcome.finalCapital()).isEqualByComparingTo("1000");
    }
}
