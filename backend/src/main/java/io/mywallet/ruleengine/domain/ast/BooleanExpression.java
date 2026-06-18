package io.mywallet.ruleengine.domain.ast;

/**
 * A rule expression that evaluates to true/false - the top level of any trading rule
 * (e.g. the whole "SMA(...) &gt; SMA(...) AND RSI(...) &lt; 70" expression).
 */
public sealed interface BooleanExpression extends RuleNode
    permits BooleanExpression.Comparison, BooleanExpression.LogicalAnd, BooleanExpression.LogicalOr, BooleanExpression.LogicalNot {

    record Comparison(NumericExpression left, ComparisonOperator operator, NumericExpression right) implements BooleanExpression {
    }

    record LogicalAnd(BooleanExpression left, BooleanExpression right) implements BooleanExpression {
    }

    record LogicalOr(BooleanExpression left, BooleanExpression right) implements BooleanExpression {
    }

    record LogicalNot(BooleanExpression operand) implements BooleanExpression {
    }

    enum ComparisonOperator {
        GREATER_THAN, LESS_THAN, EQUALS, CROSSES_ABOVE, CROSSES_BELOW
    }
}
