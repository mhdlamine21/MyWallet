package io.mywallet.order.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of an {@code Order} aggregate, and the transitions allowed between
 * them. This is the single source of truth for "can this order move from state A to
 * state B" - used both by the aggregate itself and by dedicated state-transition tests.
 */
public enum OrderStatus {

    CREATED,
    VALIDATED,
    SUBMITTED,
    PARTIALLY_FILLED,
    FILLED,
    REJECTED,
    CANCEL_REQUESTED,
    CANCELLED,
    EXPIRED;

    private static final Set<OrderStatus> TERMINAL = EnumSet.of(FILLED, REJECTED, CANCELLED, EXPIRED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == PARTIALLY_FILLED || target == FILLED || target == CANCELLED
                || target == VALIDATED || target == REJECTED;
            case VALIDATED -> target == SUBMITTED || target == REJECTED || target == CANCELLED;
            case SUBMITTED -> target == PARTIALLY_FILLED || target == FILLED
                || target == CANCEL_REQUESTED || target == CANCELLED || target == EXPIRED;
            case PARTIALLY_FILLED -> target == PARTIALLY_FILLED || target == FILLED
                || target == CANCEL_REQUESTED || target == CANCELLED || target == EXPIRED;
            case CANCEL_REQUESTED -> target == CANCELLED || target == FILLED || target == PARTIALLY_FILLED;
            case FILLED, REJECTED, CANCELLED, EXPIRED -> false; // terminal states, no way out
        };
    }
}
