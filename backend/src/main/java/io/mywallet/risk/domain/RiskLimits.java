package io.mywallet.risk.domain;

import java.math.BigDecimal;

/**
 * A portfolio's configured limits. Any field left {@code null} means "not configured for
 * this portfolio" - the corresponding check passes automatically rather than treating an
 * absent configuration as zero (which would reject every order). Defaults are set on
 * portfolio creation in a later phase; Phase 7 lets them be null (no limit) unless
 * explicitly configured.
 */
public record RiskLimits(
    BigDecimal maxOrderValue,
    BigDecimal maxExposurePerAssetFraction, // e.g. 0.25 = 25% of portfolio value
    Integer maxOrdersPerDay
) {
    public static RiskLimits none() {
        return new RiskLimits(null, null, null);
    }
}
