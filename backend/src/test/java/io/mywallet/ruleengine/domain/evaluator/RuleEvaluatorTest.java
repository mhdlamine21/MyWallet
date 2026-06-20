package io.mywallet.ruleengine.domain.evaluator;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.parser.RuleParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {

    private static List<BigDecimal> prices(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    private RuleEvaluator evaluatorWith(Map<String, List<BigDecimal>> histories, BigDecimal exposure) {
        MarketContext context = new MarketContext() {
            @Override
            public List<BigDecimal> priceHistory(String symbol) {
                return histories.get(symbol);
            }

            @Override
            public BigDecimal portfolioExposure() {
                return exposure;
            }
        };
        return new RuleEvaluator(context);
    }

    @Test
    void evaluatesASimpleGreaterThanComparison() {
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(10, 20, 30, 40, 50)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 3) > 20"); // sma of [30,40,50] = 40

        assertThat(evaluator.evaluate(expr)).isTrue();
    }

    @Test
    void evaluatesAndCorrectly() {
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(10, 20, 30, 40, 50)), new BigDecimal("0.20"));
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 3) > 20 AND PortfolioExposure < 50%");

        assertThat(evaluator.evaluate(expr)).isTrue();
    }

    @Test
    void evaluatesAndShortCircuitStyleFalseWhenOneSideFails() {
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(10, 20, 30, 40, 50)), new BigDecimal("0.80"));
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 3) > 20 AND PortfolioExposure < 50%");

        assertThat(evaluator.evaluate(expr)).isFalse();
    }

    @Test
    void evaluatesNot() {
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(10, 20, 30)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("NOT (SMA(BTCUSDT, 3) > 1000)");

        assertThat(evaluator.evaluate(expr)).isTrue();
    }

    @Test
    void detectsCrossesAbove() {
        // SMA(2) of last two points vs SMA(4) of last four points.
        // History: [10, 10, 10, 10, 30] -> previous tick (drop last): [10,10,10,10]
        //   previous SMA(2) = avg(10,10) = 10 ; previous SMA(4) = avg(10,10,10,10) = 10 -> equal, not above
        // Current tick (full): [10,10,10,10,30]
        //   current SMA(2) = avg(10,30) = 20 ; current SMA(4) = avg(10,10,10,30) = 15 -> now above
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(10, 10, 10, 10, 30)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 2) CROSSES_ABOVE SMA(BTCUSDT, 4)");

        assertThat(evaluator.evaluate(expr)).isTrue();
    }

    @Test
    void detectsCrossesAboveWhenPreviousValuesWereExactlyEqual() {
        // Equal-then-strictly-above counts as a valid crossing per the <=/> convention.
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(50, 50, 50, 50, 51)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 1) CROSSES_ABOVE SMA(BTCUSDT, 4)");

        // previous SMA(1) = 50 (last of [50,50,50,50]) vs previous SMA(4) = 50 -> equal (<=), counts as "not yet above"
        // current SMA(1) = 51 vs current SMA(4) = avg(50,50,50,51) = 50.25 -> now above
        assertThat(evaluator.evaluate(expr)).isTrue();
    }

    @Test
    void doesNotDetectCrossesAboveWhenNeverCrossing() {
        // Short is above long on both the previous and current tick -> sustained, not crossing.
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(100, 100, 100, 100, 100)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 1) CROSSES_ABOVE SMA(BTCUSDT, 1)");

        assertThat(evaluator.evaluate(expr)).isFalse();
    }

    @Test
    void evaluatesEqualsComparison() {
        var evaluator = evaluatorWith(Map.of("BTCUSDT", prices(5, 5, 5)), BigDecimal.ZERO);
        BooleanExpression expr = RuleParser.parse("SMA(BTCUSDT, 3) == 5");

        assertThat(evaluator.evaluate(expr)).isTrue();
    }
}
