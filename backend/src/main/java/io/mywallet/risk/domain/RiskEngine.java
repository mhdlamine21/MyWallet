package io.mywallet.risk.domain;

import java.util.List;

/**
 * Runs every configured {@link RiskCheck} against a context and reports every breach -
 * not just the first one, so a rejected order's response can explain all the reasons at
 * once rather than making the caller fix one violation at a time.
 */
public final class RiskEngine {

    private final List<RiskCheck> checks;

    public RiskEngine(List<RiskCheck> checks) {
        this.checks = List.copyOf(checks);
    }

    public static RiskEngine withDefaultChecks() {
        return new RiskEngine(List.of(
            new io.mywallet.risk.domain.check.AvailableCashForBuyCheck(),
            new io.mywallet.risk.domain.check.AvailableQuantityForSaleCheck(),
            new io.mywallet.risk.domain.check.MaxOrderValueCheck(),
            new io.mywallet.risk.domain.check.MaxExposurePerAssetCheck(),
            new io.mywallet.risk.domain.check.MaxOrdersPerDayCheck()
        ));
    }

    public RiskAssessment assess(RiskCheckContext context) {
        List<RiskCheckResult> results = checks.stream().map(check -> check.evaluate(context)).toList();
        List<RiskCheckResult> breaches = results.stream().filter(r -> !r.passed()).toList();
        return new RiskAssessment(breaches.isEmpty(), breaches, riskScore(breaches));
    }

    /**
     * A simple, explainable score: each breach contributes a fixed amount, capped at 100.
     * Deliberately not a black-box model - every point is traceable to a specific limit
     * type, matching the project's "chaque alerte doit expliquer clairement sa cause"
     * requirement at the aggregate level too, not just per-check.
     */
    private int riskScore(List<RiskCheckResult> breaches) {
        return Math.min(100, breaches.size() * 35);
    }

    public record RiskAssessment(boolean accepted, List<RiskCheckResult> breaches, int riskScore) {

        public RiskLevel level() {
            if (riskScore >= 90) return RiskLevel.CRITICAL;
            if (riskScore >= 60) return RiskLevel.HIGH;
            if (riskScore >= 30) return RiskLevel.MEDIUM;
            return RiskLevel.LOW;
        }

        public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
    }
}
