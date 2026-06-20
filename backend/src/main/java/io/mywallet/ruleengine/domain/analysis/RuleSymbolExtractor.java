package io.mywallet.ruleengine.domain.analysis;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.ast.NumericExpression;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Walks a {@link BooleanExpression} AST and collects every asset symbol referenced by its
 * {@code IndicatorCall} nodes - this is how {@code StrategyExecutionScheduler} knows which
 * asset a stored rule actually trades, without re-parsing symbol names out of the raw
 * expression string. Same "pattern-matching switch over a closed sealed hierarchy" shape
 * as {@code RuleEvaluator}, reused here for a different purpose (static analysis rather
 * than evaluation).
 *
 * <p>Consistent with the rest of this codebase's single-asset simplification (see
 * {@code BacktestMarketContext}): a rule is expected to reference exactly one symbol.
 * {@link #extractSingleSymbol} enforces that expectation explicitly rather than silently
 * picking "the first one found" if a rule ever names two.</p>
 */
public final class RuleSymbolExtractor {

    private RuleSymbolExtractor() {
    }

    public static Set<String> extractSymbols(BooleanExpression expression) {
        Set<String> symbols = new LinkedHashSet<>();
        collect(expression, symbols);
        return symbols;
    }

    /**
     * @throws IllegalArgumentException if the rule references zero or more than one
     *         distinct symbol - the scheduler needs exactly one to know what to trade.
     */
    public static String extractSingleSymbol(BooleanExpression expression) {
        Set<String> symbols = extractSymbols(expression);
        if (symbols.isEmpty()) {
            throw new IllegalArgumentException("Rule references no indicator/symbol - nothing to trade automatically");
        }
        if (symbols.size() > 1) {
            throw new IllegalArgumentException(
                "Rule references multiple symbols " + symbols + " - automatic execution supports single-asset rules only");
        }
        return symbols.iterator().next();
    }

    private static void collect(BooleanExpression expression, Set<String> out) {
        switch (expression) {
            case BooleanExpression.LogicalAnd e -> {
                collect(e.left(), out);
                collect(e.right(), out);
            }
            case BooleanExpression.LogicalOr e -> {
                collect(e.left(), out);
                collect(e.right(), out);
            }
            case BooleanExpression.LogicalNot e -> collect(e.operand(), out);
            case BooleanExpression.Comparison e -> {
                collect(e.left(), out);
                collect(e.right(), out);
            }
        }
    }

    private static void collect(NumericExpression expression, Set<String> out) {
        if (expression instanceof NumericExpression.IndicatorCall call) {
            out.add(call.symbol());
        }
        // Literal and PortfolioMetric carry no symbol - nothing to collect.
    }
}
