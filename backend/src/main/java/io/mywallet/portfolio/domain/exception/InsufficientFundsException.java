package io.mywallet.portfolio.domain.exception;

import io.mywallet.common.exception.DomainException;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "INSUFFICIENT_FUNDS";
    }
}
