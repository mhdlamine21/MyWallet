package io.mywallet.ruleengine.domain.evaluator;

import io.mywallet.ruleengine.domain.ast.BooleanExpression;
import io.mywallet.ruleengine.domain.ast.NumericExpression;
import io.mywallet.ruleengine.domain.indicator.TechnicalIndicators;

import java.math.BigDecimal;
import java.util.List;

/**
 * Walks a {@link BooleanExpression} AST and computes its boolean result against a
 * {@link MarketContext}.
 *
 * =========================================================================================
 * NOTE D'APPRENTISSAGE ÉTUDIANT (Semaine 6 - Sécurité : Le cauchemar du eval() évité)
 * =========================================================================================
 * - Idée naïve de départ (Semaine 4) :
 *   Pour évaluer des règles comme "SMA(BTC, 20) > 50000", je pensais concaténer la chaîne
 *   et utiliser un moteur JavaScript comme Nashorn ou SpEL (`ExpressionParser.parseExpression()`).
 * - Danger découvert en cours de cybersécurité :
 *   Injection de code à distance (RCE) ! Un pirate aurait pu entrer :
 *   `T(java.lang.Runtime).getRuntime().exec('rm -rf /')` et détruire le serveur.
 * - Solution propre & sécurisée (Semaine 6) :
 *   Construction d'un parser récursif descendant qui génère un véritable Arbre de Syntaxe Abstraite (AST).
 *   Ici, `RuleEvaluator` parcourt uniquement les nœuds scellés (sealed interfaces Java 21) via un
 *   `switch pattern matching` exhaustif. Impossible d'exécuter du code arbitraire !
 * =========================================================================================
 */
public final class RuleEvaluator {

    private final MarketContext context;

    public RuleEvaluator(MarketContext context) {
        this.context = context;
    }

    public boolean evaluate(BooleanExpression expression) {
        return switch (expression) {
            case BooleanExpression.LogicalAnd e -> evaluate(e.left()) && evaluate(e.right());
            case BooleanExpression.LogicalOr e -> evaluate(e.left()) || evaluate(e.right());
            case BooleanExpression.LogicalNot e -> !evaluate(e.operand());
            case BooleanExpression.Comparison e -> evaluateComparison(e);
        };
    }

    private boolean evaluateComparison(BooleanExpression.Comparison comparison) {
        return switch (comparison.operator()) {
            case GREATER_THAN -> evaluateNumeric(comparison.left(), 0).compareTo(evaluateNumeric(comparison.right(), 0)) > 0;
            case LESS_THAN -> evaluateNumeric(comparison.left(), 0).compareTo(evaluateNumeric(comparison.right(), 0)) < 0;
            case EQUALS -> evaluateNumeric(comparison.left(), 0).compareTo(evaluateNumeric(comparison.right(), 0)) == 0;
            case CROSSES_ABOVE -> crosses(comparison.left(), comparison.right(), true);
            case CROSSES_BELOW -> crosses(comparison.left(), comparison.right(), false);
        };
    }

    /**
     * A crosses above B if, one tick ago, A was &lt;= B, and now A &gt; B (symmetric for
     * crosses below). Requires re-evaluating both sides one tick in the past - see
     * {@link #evaluateNumeric(NumericExpression, int)}'s {@code offset} parameter.
     */
    private boolean crosses(NumericExpression left, NumericExpression right, boolean above) {
        BigDecimal previousLeft = evaluateNumeric(left, 1);
        BigDecimal previousRight = evaluateNumeric(right, 1);
        BigDecimal currentLeft = evaluateNumeric(left, 0);
        BigDecimal currentRight = evaluateNumeric(right, 0);

        if (above) {
            return previousLeft.compareTo(previousRight) <= 0 && currentLeft.compareTo(currentRight) > 0;
        }
        return previousLeft.compareTo(previousRight) >= 0 && currentLeft.compareTo(currentRight) < 0;
    }

    /**
     * @param offset 0 = evaluate at the current tick, 1 = evaluate one tick in the past
     *               (used by {@link #crosses}) - implemented by dropping the last
     *               {@code offset} points from whatever price history the expression reads.
     */
    private BigDecimal evaluateNumeric(NumericExpression expression, int offset) {
        return switch (expression) {
            case NumericExpression.Literal e -> e.value();
            case NumericExpression.IndicatorCall e -> evaluateIndicator(e, offset);
            case NumericExpression.PortfolioMetric e -> evaluatePortfolioMetric(e, offset);
        };
    }

    private BigDecimal evaluateIndicator(NumericExpression.IndicatorCall call, int offset) {
        List<BigDecimal> fullHistory = context.priceHistory(call.symbol());
        List<BigDecimal> history = offset == 0 ? fullHistory : truncateForOffset(fullHistory, offset, call.symbol());

        return switch (call.type()) {
            case SMA -> TechnicalIndicators.sma(history, call.period());
            case EMA -> TechnicalIndicators.ema(history, call.period());
            case RSI -> TechnicalIndicators.rsi(history, call.period());
        };
    }

    private List<BigDecimal> truncateForOffset(List<BigDecimal> fullHistory, int offset, String symbol) {
        if (fullHistory.size() <= offset) {
            throw new IllegalArgumentException(
                "Not enough price history for symbol %s to evaluate a CROSSES_ABOVE/CROSSES_BELOW comparison (need at least %d points, got %d)"
                    .formatted(symbol, offset + 1, fullHistory.size()));
        }
        return fullHistory.subList(0, fullHistory.size() - offset);
    }

    private BigDecimal evaluatePortfolioMetric(NumericExpression.PortfolioMetric metric, int offset) {
        if (offset != 0) {
            throw new UnsupportedOperationException(
                "CROSSES_ABOVE/CROSSES_BELOW against a portfolio metric is not supported in this phase " +
                "(no historical series is tracked for portfolio metrics yet - only for indicator price series).");
        }
        return switch (metric.type()) {
            case EXPOSURE -> context.portfolioExposure();
            case DAILY_LOSS, CONSECUTIVE_LOSSES -> throw new UnsupportedOperationException(
                metric.type() + " is parseable but has no data source until RiskEngine (Phase 7) provides it.");
        };
    }
}
