package io.mywallet.infrastructure.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Single place that knows the WebSocket topic naming scheme (see {@link WebSocketConfig}
 * for the full list) - callers (the market-data generator, risk assessment, order
 * services) don't build destination strings themselves, matching the same
 * "one component owns the naming convention" pattern as {@code DomainEventPublisher} for
 * RabbitMQ routing keys.
 *
 * <p>Broadcasting failures never propagate to the caller - a WebSocket push is a
 * best-effort convenience for connected clients, not a source of truth (that's always the
 * database), so a broadcast failure here must never fail the business operation that
 * triggered it.</p>
 */
@Component
public class WebSocketBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(WebSocketBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public record PriceTick(String symbol, BigDecimal price, Instant observedAt) {}

    public record RiskAlertMessage(UUID portfolioId, String limitType, String level, int riskScore, String explanation) {}

    public record OrderStatusMessage(UUID orderId, UUID portfolioId, String status, String filledQuantity) {}

    public record LeaderboardEntry(UUID strategyId, String strategyName, BigDecimal currentValue,
                                    BigDecimal baselineValue, BigDecimal returnFraction) {}

    public record MarketAnomalyMessage(String symbol, BigDecimal price, BigDecimal zScore, String explanation) {}

    public void broadcastPrice(String symbol, BigDecimal price, Instant observedAt) {
        safeSend("/topic/prices/" + symbol, new PriceTick(symbol, price, observedAt));
    }

    public void broadcastRiskAlert(UUID portfolioId, String limitType, String level, int riskScore, String explanation) {
        safeSend("/topic/portfolios/" + portfolioId + "/risk-alerts",
            new RiskAlertMessage(portfolioId, limitType, level, riskScore, explanation));
    }

    public void broadcastOrderStatus(UUID orderId, UUID portfolioId, String status, String filledQuantity) {
        safeSend("/topic/portfolios/" + portfolioId + "/orders",
            new OrderStatusMessage(orderId, portfolioId, status, filledQuantity));
    }

    public void broadcastLeaderboard(java.util.List<LeaderboardEntry> entries) {
        safeSend("/topic/leaderboard", entries);
    }

    public void broadcastMarketAnomaly(String symbol, BigDecimal price, BigDecimal zScore, String explanation) {
        safeSend("/topic/anomalies/" + symbol, new MarketAnomalyMessage(symbol, price, zScore, explanation));
    }

    private void safeSend(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast to {} - connected clients may miss this update, but no business data was lost", destination, e);
        }
    }
}
