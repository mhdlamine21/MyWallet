package io.mywallet.strategy.domain;

import java.util.EnumSet;
import java.util.Set;

public enum StrategyStatus {
    DRAFT, ACTIVE, SUSPENDED, DEACTIVATED;

    private static final Set<StrategyStatus> DEACTIVATED_TERMINAL = EnumSet.of(DEACTIVATED);

    public boolean canTransitionTo(StrategyStatus target) {
        if (DEACTIVATED_TERMINAL.contains(this)) {
            return false; // DEACTIVATED is terminal - no reactivation, matches audit-trail expectations
        }
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == DEACTIVATED;
            case ACTIVE -> target == SUSPENDED || target == DEACTIVATED;
            case SUSPENDED -> target == ACTIVE || target == DEACTIVATED;
            case DEACTIVATED -> false;
        };
    }
}
