package io.mywallet.ruleengine.domain.ast;

import java.math.BigDecimal;

/**
 * A rule expression that evaluates to a number - either a literal, a technical indicator
 * call (which needs a symbol and a period), or a portfolio-state reference like
 * {@code PortfolioExposure}.
 */
public sealed interface NumericExpression extends RuleNode
    permits NumericExpression.Literal, NumericExpression.IndicatorCall, NumericExpression.PortfolioMetric {

    record Literal(BigDecimal value) implements NumericExpression {
    }

    record IndicatorCall(IndicatorType type, String symbol, int period) implements NumericExpression {
    }

    record PortfolioMetric(PortfolioMetricType type) implements NumericExpression {
    }

    enum IndicatorType {
        SMA, EMA, RSI
        // MACD, BOLLINGER_UPPER, BOLLINGER_LOWER, ATR, VWAP, OBV: parseable as tokens
        // (see RuleLexer) but rejected by RuleParser with a clear "not yet implemented"
        // error - documented Phase 6b scope, not silently mis-evaluated.
    }

    enum PortfolioMetricType {
        EXPOSURE, DAILY_LOSS, CONSECUTIVE_LOSSES
        // Only EXPOSURE has a computed source in Phase 6 (see RuleEvaluator) - the other
        // two are parseable but throw at evaluation time until RiskEngine (Phase 7)
        // provides real data for them.
    }
}
