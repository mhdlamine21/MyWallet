package io.mywallet.risk.application.exception;

import io.mywallet.common.exception.DomainException;
import io.mywallet.risk.domain.RiskCheckResult;

import java.util.List;

public class RiskLimitBreachedException extends DomainException {

    private final List<RiskCheckResult> breaches;

    public RiskLimitBreachedException(List<RiskCheckResult> breaches) {
        super(breaches.stream().map(RiskCheckResult::explanation).reduce((a, b) -> a + "; " + b).orElse("Risk limit breached"));
        this.breaches = breaches;
    }

    public List<RiskCheckResult> breaches() {
        return breaches;
    }

    @Override
    public String errorCode() {
        return "RISK_LIMIT_BREACHED";
    }
}
