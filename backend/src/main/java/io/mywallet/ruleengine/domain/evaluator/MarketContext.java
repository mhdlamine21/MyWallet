package io.mywallet.ruleengine.domain.evaluator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything {@link RuleEvaluator} needs to resolve indicator calls and portfolio metrics,
 * supplied by the caller (the application layer, backed by real price/portfolio data) -
 * the domain evaluator itself has zero I/O and zero framework dependency, consistent with
 * every other pure-domain piece in this codebase.
 */
public interface MarketContext {

    /**
     * Chronologically ordered (oldest first) closing prices for {@code symbol}, ending at
     * the current evaluation point. Must contain enough history for whatever indicator
     * period is requested, or the indicator calculation throws.
     */
    List<BigDecimal> priceHistory(String symbol);

    /** Current portfolio exposure as a fraction (0.25 = 25%), not a percentage. */
    BigDecimal portfolioExposure();
}
