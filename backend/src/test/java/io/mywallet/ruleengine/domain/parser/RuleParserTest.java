package io.mywallet.ruleengine.domain.parser;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.ast.NumericExpression;
import io.mywallet.ruleengine.domain.exception.RuleParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleParserTest {

    @Test
    void parsesTheReferenceExampleFromTheProjectBrief() {
        BooleanExpression result = RuleParser.parse(
            "SMA(BTCUSDT, 20) > SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) < 70 AND PortfolioExposure < 50%"
        );

        // AND is left-associative: ((A AND B) AND C)
        assertThat(result).isInstanceOf(BooleanExpression.LogicalAnd.class);
        var outer = (BooleanExpression.LogicalAnd) result;
        assertThat(outer.right()).isInstanceOf(BooleanExpression.Comparison.class);
        var exposureComparison = (BooleanExpression.Comparison) outer.right();
        var exposureLiteral = (NumericExpression.Literal) exposureComparison.right();
        // 50% parses to 0.50, not 50 - percentages are fractions internally.
        assertThat(exposureLiteral.value()).isEqualByComparingTo("0.50");
    }

    @Test
    void parsesASimpleComparison() {
        var result = (BooleanExpression.Comparison) RuleParser.parse("SMA(ETHUSDT, 10) > 100");
        var indicator = (NumericExpression.IndicatorCall) result.left();
        assertThat(indicator.type()).isEqualTo(NumericExpression.IndicatorType.SMA);
        assertThat(indicator.symbol()).isEqualTo("ETHUSDT");
        assertThat(indicator.period()).isEqualTo(10);
        assertThat(result.operator()).isEqualTo(BooleanExpression.ComparisonOperator.GREATER_THAN);
    }

    @Test
    void parsesCrossesAboveAndBelow() {
        var above = RuleParser.parse("SMA(BTCUSDT, 20) CROSSES_ABOVE SMA(BTCUSDT, 50)");
        assertThat(((BooleanExpression.Comparison) above).operator()).isEqualTo(BooleanExpression.ComparisonOperator.CROSSES_ABOVE);

        var below = RuleParser.parse("SMA(BTCUSDT, 20) CROSSES_BELOW SMA(BTCUSDT, 50)");
        assertThat(((BooleanExpression.Comparison) below).operator()).isEqualTo(BooleanExpression.ComparisonOperator.CROSSES_BELOW);
    }

    @Test
    void parsesNotAndParentheses() {
        var result = RuleParser.parse("NOT (RSI(BTCUSDT, 14) > 70)");
        assertThat(result).isInstanceOf(BooleanExpression.LogicalNot.class);
    }

    @Test
    void orHasLowerPrecedenceThanAnd() {
        // A OR B AND C should parse as A OR (B AND C)
        var result = (BooleanExpression.LogicalOr) RuleParser.parse(
            "RSI(BTCUSDT, 14) > 70 OR SMA(BTCUSDT, 20) > 100 AND SMA(BTCUSDT, 50) > 100"
        );
        assertThat(result.right()).isInstanceOf(BooleanExpression.LogicalAnd.class);
    }

    @Test
    void rejectsUnknownIndicatorRatherThanExecutingArbitraryCode() {
        assertThatThrownBy(() -> RuleParser.parse("MACD(BTCUSDT, 12) > 0"))
            .isInstanceOf(RuleParseException.class)
            .hasMessageContaining("MACD");
    }

    @Test
    void rejectsUnknownIdentifierUsedAsPortfolioMetric() {
        assertThatThrownBy(() -> RuleParser.parse("SomeRandomThing > 5"))
            .isInstanceOf(RuleParseException.class);
    }

    @Test
    void rejectsGarbageCharactersOutright() {
        assertThatThrownBy(() -> RuleParser.parse("SMA(BTCUSDT, 20) > 100; System.exit(1)"))
            .isInstanceOf(RuleParseException.class);
    }

    @Test
    void rejectsUnclosedParenthesis() {
        assertThatThrownBy(() -> RuleParser.parse("(SMA(BTCUSDT, 20) > 100"))
            .isInstanceOf(RuleParseException.class);
    }

    @Test
    void rejectsTrailingGarbageAfterAValidExpression() {
        assertThatThrownBy(() -> RuleParser.parse("SMA(BTCUSDT, 20) > 100 extra_tokens_here"))
            .isInstanceOf(RuleParseException.class);
    }

    @Test
    void rejectsNegativeOrZeroPeriod() {
        assertThatThrownBy(() -> RuleParser.parse("SMA(BTCUSDT, -5) > 100")).isInstanceOf(RuleParseException.class);
        assertThatThrownBy(() -> RuleParser.parse("SMA(BTCUSDT, 0) > 100")).isInstanceOf(RuleParseException.class);
    }

    @Test
    void rejectsEmptyExpression() {
        assertThatThrownBy(() -> RuleParser.parse("")).isInstanceOf(RuleParseException.class);
    }
}
