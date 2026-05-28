package io.mywallet.infrastructure.web;

import io.mywallet.common.dto.ErrorResponse;
import io.mywallet.common.exception.DomainException;
import io.mywallet.common.exception.OptimisticConcurrencyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Single place that decides how every exception becomes an HTTP response. Business-rule
 * violations ({@link DomainException} subtypes) map to 4xx with their own error code;
 * anything unanticipated maps to a generic 500 that never leaks internal details (stack
 * traces, SQL, etc.) to the caller - those go to the server logs instead, tagged with the
 * same correlation id the caller sees, so a bug report referencing the id is enough to
 * find the full trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(io.mywallet.infrastructure.admin.KillSwitchActiveException.class)
    public ResponseEntity<ErrorResponse> handleKillSwitchActive(
        io.mywallet.infrastructure.admin.KillSwitchActiveException ex, HttpServletRequest request) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.errorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        HttpStatus status = ex instanceof OptimisticConcurrencyException
            ? HttpStatus.CONFLICT
            : HttpStatus.UNPROCESSABLE_ENTITY;
        return respond(status, ex.errorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()))
            .orElse("Validation failed");
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(io.mywallet.ruleengine.domain.exception.RuleParseException.class)
    public ResponseEntity<ErrorResponse> handleRuleParseException(
        io.mywallet.ruleengine.domain.exception.RuleParseException ex, HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RULE_EXPRESSION", ex.getMessage(), request);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(java.util.NoSuchElementException ex, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        // Deliberately the exact same message/code regardless of whether the email exists
        // or the password was wrong - see auth's user-enumeration protection notes.
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return respond(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String correlationId = correlationId(request);
        log.error("Unhandled exception [correlationId={}]", correlationId, ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message, HttpServletRequest request) {
        String correlationId = correlationId(request);
        return ResponseEntity.status(status).body(ErrorResponse.of(code, message, correlationId));
    }

    private String correlationId(HttpServletRequest request) {
        Object fromMdc = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
        return fromMdc != null ? fromMdc.toString() : request.getHeader(CorrelationIdFilter.HEADER_NAME);
    }
}
