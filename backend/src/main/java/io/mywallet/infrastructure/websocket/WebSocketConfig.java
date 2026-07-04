package io.mywallet.infrastructure.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

import java.util.List;

/**
 * STOMP endpoint at {@code /ws}. Topics published by {@link WebSocketBroadcaster}:
 * <ul>
 *   <li>{@code /topic/prices/{symbol}} - every simulated price tick</li>
 *   <li>{@code /topic/portfolios/{portfolioId}/risk-alerts} - new RiskAlerts as they're raised</li>
 *   <li>{@code /topic/portfolios/{portfolioId}/orders} - order status changes</li>
 * </ul>
 *
 * <p>Uses Spring's in-memory simple broker rather than relaying through RabbitMQ's own
 * STOMP plugin - simpler to operate for this project's scale, and keeps RabbitMQ's role
 * exactly what ADR-0002 describes (durable async fan-out between backend components), not
 * also a browser-facing pub/sub layer.</p>
 *
 * <p>Two things this class deliberately restricts, both corrected during the
 * `/security-codeguard-agent` pass: the browser origin allowed to connect (was {@code *},
 * now the same allow-list as the HTTP CORS config), and - via
 * {@link StompSubscriptionAuthorizationInterceptor} on the inbound channel - which
 * portfolio-scoped topics an authenticated connection may actually subscribe to.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompSubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
        JwtHandshakeInterceptor jwtHandshakeInterceptor,
        StompSubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor,
        @Value("${mywallet.cors.allowed-origins:http://localhost:5173}") List<String> allowedOrigins
    ) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.subscriptionAuthorizationInterceptor = subscriptionAuthorizationInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .addInterceptors(jwtHandshakeInterceptor)
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
        // No SockJS fallback: this targets modern browsers only, which all support native
        // WebSocket - SockJS's XHR-polling fallback exists for old-browser/proxy
        // compatibility this project doesn't need, and dropping it keeps both the backend
        // config and the frontend client (@stomp/stompjs over plain WebSocket) simpler.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionAuthorizationInterceptor);
    }
}
