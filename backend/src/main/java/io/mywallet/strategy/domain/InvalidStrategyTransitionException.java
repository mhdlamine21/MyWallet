package io.mywallet.strategy.domain;

import io.mywallet.common.exception.DomainException;

public class InvalidStrategyTransitionException extends DomainException {

    public InvalidStrategyTransitionException(StrategyStatus from, StrategyStatus to) {
        super("Cannot transition strategy from %s to %s".formatted(from, to));
    }

    @Override
    public String errorCode() {
        return "INVALID_STRATEGY_TRANSITION";
    }
}
