package io.mywallet.order.domain.exception;

import io.mywallet.common.exception.DomainException;
import io.mywallet.order.domain.model.OrderStatus;

public class InvalidOrderStateTransitionException extends DomainException {

    public InvalidOrderStateTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot transition order from %s to %s".formatted(from, to));
    }

    @Override
    public String errorCode() {
        return "ORDER_INVALID_STATE_TRANSITION";
    }
}
