package io.mywallet.infrastructure.websocket;

import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Closes the gap flagged in {@code docs/architecture/known-limitations.md} after Phase
 * 11: {@link JwtHandshakeInterceptor} proves a connection belongs to a real logged-in
 * user, but on its own that's not enough - without this interceptor, any authenticated
 * user who learns or guesses another user's portfolio UUID could subscribe to
 * {@code /topic/portfolios/{portfolioId}/...} and see their private risk alerts and order
 * activity. This runs on every inbound STOMP frame and rejects a {@code SUBSCRIBE} to a
 * portfolio-scoped topic unless the connected user (resolved from the session attribute
 * {@link JwtHandshakeInterceptor} set at handshake time) actually owns that portfolio -
 * the same "you can't see what you don't own" rule already enforced on every REST
 * endpoint, applied here to the one channel that didn't have it yet.
 */
@Component
public class StompSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompSubscriptionAuthorizationInterceptor.class);
    private static final Pattern PORTFOLIO_TOPIC_PATTERN = Pattern.compile("^/topic/portfolios/([0-9a-fA-F-]{36})/.*$");

    private final PortfolioProjectionJpaRepository portfolioRepository;
    private final AccountJpaRepository accountRepository;

    public StompSubscriptionAuthorizationInterceptor(
        PortfolioProjectionJpaRepository portfolioRepository,
        AccountJpaRepository accountRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message; // only SUBSCRIBE frames need this check
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Matcher matcher = PORTFOLIO_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return message; // not a portfolio-scoped topic (e.g. /topic/prices/...) - no ownership to check
        }

        UUID portfolioId;
        try {
            portfolioId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException notAUuid) {
            return blockSubscription(destination, "malformed portfolio id");
        }

        Object rawUserId = accessor.getSessionAttributes() != null
            ? accessor.getSessionAttributes().get(JwtHandshakeInterceptor.USER_ID_SESSION_ATTRIBUTE)
            : null;
        if (rawUserId == null) {
            return blockSubscription(destination, "no authenticated user on this session");
        }
        UUID userId = UUID.fromString(rawUserId.toString());

        boolean owns = portfolioRepository.findById(portfolioId)
            .flatMap(portfolio -> accountRepository.findById(portfolio.getAccountId()))
            .map(account -> account.getOwnerId().equals(userId))
            .orElse(false);

        if (!owns) {
            return blockSubscription(destination, "user " + userId + " does not own portfolio " + portfolioId);
        }

        return message;
    }

    private Message<?> blockSubscription(String destination, String reason) {
        log.warn("Blocked unauthorized STOMP subscription to {} - {}", destination, reason);
        return null; // returning null from preSend cancels the message - the SUBSCRIBE never reaches the broker
    }
}
