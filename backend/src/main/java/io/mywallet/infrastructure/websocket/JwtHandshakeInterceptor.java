package io.mywallet.infrastructure.websocket;

import io.jsonwebtoken.Claims;
import io.mywallet.auth.infrastructure.security.JwtTokenProvider;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

/**
 * A native browser WebSocket upgrade request cannot carry a custom {@code Authorization}
 * header (unlike a normal XHR/fetch call), so the JWT is passed as a query parameter
 * instead (e.g. {@code /ws?token=...}) and validated here, at the HTTP handshake stage,
 * before the connection is ever accepted. The resolved user id is stashed in the WebSocket
 * session attributes for later use (e.g. per-user destinations).
 *
 * <p>{@code SecurityConfig} permits {@code /ws/**} at the HTTP filter-chain level - the
 * normal {@code JwtAuthenticationFilter} never sees this request, so this interceptor is
 * the only thing standing between an anonymous handshake and a live connection. This is a
 * deliberate, minimal implementation: it authenticates the connection, but does not yet
 * authorize which portfolio-scoped topics a given connection may subscribe to (any
 * authenticated user can currently subscribe to any portfolio's topic if they guess its
 * UUID) - flagged in known-limitations.md as a `/security-codeguard-agent` follow-up.</p>
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_SESSION_ATTRIBUTE = "userId";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        Optional<Claims> claims = jwtTokenProvider.parse(token);
        if (claims.isEmpty()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        attributes.put(USER_ID_SESSION_ATTRIBUTE, jwtTokenProvider.userIdFrom(claims.get()).toString());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after a successful/failed handshake.
    }

    private String extractToken(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        return servletRequest.getServletRequest().getParameter("token");
    }
}
