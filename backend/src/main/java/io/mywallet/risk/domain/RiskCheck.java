package io.mywallet.risk.domain;

public interface RiskCheck {
    RiskCheckResult evaluate(RiskCheckContext context);

    /** Stable identifier matching a {@code RiskLimits} field / {@code risk_limits.limit_type}. */
    String limitType();
}
