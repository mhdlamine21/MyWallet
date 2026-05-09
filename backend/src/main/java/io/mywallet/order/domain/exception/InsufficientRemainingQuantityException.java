package io.mywallet.order.domain.exception;

import io.mywallet.common.exception.DomainException;

import java.math.BigDecimal;

public class InsufficientRemainingQuantityException extends DomainException {

    public InsufficientRemainingQuantityException(BigDecimal remaining, BigDecimal requested) {
        super("Cannot fill %s units, only %s remaining on this order".formatted(requested, remaining));
    }

    @Override
    public String errorCode() {
        return "ORDER_INSUFFICIENT_REMAINING_QUANTITY";
    }
}
