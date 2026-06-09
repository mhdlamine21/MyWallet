package io.mywallet.infrastructure.ratelimit;

import io.mywallet.common.dto.ErrorResponse;
import io.mywallet.infrastructure.web.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate-limits {@code /api/auth/**} specifically (register, login, refresh) - the
 * endpoints a brute-force or credential-stuffing attack would target, and the only ones
 * that are {@code permitAll} in {@code SecurityConfig} (so nothing else in
 * {@code JwtAuthenticationFilter}'s chain already gates repeated requests there).
 *
 * <p>Keyed by client IP rather than by account: limiting per-account would still let an
 * attacker try many different accounts from one IP; limiting per-IP (with a fairly
 * generous threshold - this protects against automated brute force, not against a
 * legitimate user mistyping their password a few times) is the right first line of
 * defense here. Account-level lockout (see {@code UserEntity.recordFailedLogin}) is the
 * second.</p>
 *
 * <p>Thresholds are configurable ({@code mywallet.ratelimit.auth.*}) rather than
 * hardcoded specifically so the test profile can raise them - {@code InMemoryRateLimiter}
 * is a singleton bean, and dozens of integration test classes across this codebase each
 * call {@code /api/auth/register} at least once from the same simulated client address
 * within a single (Spring-context-cached) test run; without a much higher test-profile
 * limit, the test suite itself would trip its own rate limiter.</p>
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final long windowMillis;
    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(
        InMemoryRateLimiter rateLimiter,
        ObjectMapper objectMapper,
        @Value("${mywallet.ratelimit.auth.max-requests:10}") int maxRequests,
        @Value("${mywallet.ratelimit.auth.window-ms:60000}") long windowMillis
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String key = "auth:" + clientIp(request);

        if (!rateLimiter.tryAcquire(key, maxRequests, windowMillis)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME);
            ErrorResponse body = ErrorResponse.of("RATE_LIMIT_EXCEEDED",
                "Too many authentication requests from this client - please wait a minute and try again.", correlationId);
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // X-Forwarded-For is only trustworthy behind a reverse proxy that overwrites it
        // (not just appends) - acceptable here since the deployment target (Nginx in
        // front, per docker-compose.prod-lite.yml) does exactly that; falls back to the
        // direct remote address for local/dev runs with no proxy in front.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
