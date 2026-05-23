package io.mywallet.order.application.exception;

import io.mywallet.common.exception.DomainException;

public class UnknownAssetException extends DomainException {

    public UnknownAssetException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "UNKNOWN_ASSET";
    }
}
