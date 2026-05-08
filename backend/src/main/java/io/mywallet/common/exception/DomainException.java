package io.mywallet.common.exception;

/**
 * Base type for all business-rule violations raised by the domain layer (as opposed to
 * infrastructure failures like a DB connection error). Interfaces map these to HTTP 4xx
 * responses with the platform's standardized error format (timestamp, code, message,
 * correlationId).
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    /** Machine-readable error code, stable across releases, used in the API error body. */
    public abstract String errorCode();
}
