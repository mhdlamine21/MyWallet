package io.mywallet.risk.domain;

/**
 * The explanation must always be concrete enough to stand alone - e.g. "Exposure to
 * BTCUSDT would be 42%, configured limit is 25%" - per the project brief's explicit
 * example of what a good {@code RiskAlert} looks like. A vague "risk limit exceeded"
 * would technically satisfy the type but fails the actual requirement.
 */
public record RiskCheckResult(boolean passed, String limitType, String explanation) {

    public static RiskCheckResult pass(String limitType) {
        return new RiskCheckResult(true, limitType, null);
    }

    public static RiskCheckResult breach(String limitType, String explanation) {
        return new RiskCheckResult(false, limitType, explanation);
    }
}
