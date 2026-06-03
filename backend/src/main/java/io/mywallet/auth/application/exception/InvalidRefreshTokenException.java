package io.mywallet.auth.application.exception;

import io.mywallet.common.exception.DomainException;

/**
 * Raised both for a plain invalid/expired refresh token and for the more serious case of
 * a <em>reused, already-rotated</em> token - the latter signals possible theft. Both cases
 * return the same generic error to the client (no need to tell an attacker which case they
 * hit); the reuse case additionally revokes the entire token family server-side, forcing
 * re-authentication. See {@code AuthenticationService#refresh}.
 */
public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired, or has already been used");
    }

    @Override
    public String errorCode() {
        return "INVALID_REFRESH_TOKEN";
    }
}
