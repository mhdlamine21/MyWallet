package io.mywallet.common.dto;

import java.time.Instant;

/**
 * The one and only error shape the API ever returns. Every {@code 4xx}/{@code 5xx}
 * response body looks like this - see {@code GlobalExceptionHandler}.
 */
public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    String correlationId
) {
    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(Instant.now(), code, message, correlationId);
    }
}
