package io.mywallet.auth.application.exception;

import io.mywallet.common.exception.DomainException;

public class EmailAlreadyInUseException extends DomainException {

    public EmailAlreadyInUseException() {
        super("An account with this email already exists");
    }

    @Override
    public String errorCode() {
        return "EMAIL_ALREADY_IN_USE";
    }
}
