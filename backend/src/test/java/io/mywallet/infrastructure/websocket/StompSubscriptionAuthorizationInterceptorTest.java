package io.mywallet.infrastructure.websocket;

import io.mywallet.account.infrastructure.persistence.AccountEntity;
import io.mywallet.account.infrastructure.persistence.AccountJpaRepository;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionEntity;
import io.mywallet.portfolio.infrastructure.persistence.PortfolioProjectionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompSubscriptionAuthorizationInterceptorTest {

    private final PortfolioProjectionJpaRepository portfolioRepository = mock(PortfolioProjectionJpaRepository.class);
    private final AccountJpaRepository accountRepository = mock(AccountJpaRepository.class);
    private final StompSubscriptionAuthorizationInterceptor interceptor =
        new StompSubscriptionAuthorizationInterceptor(portfolioRepository, accountRepository);

    private Message<byte[]> subscribeMessage(String destination, UUID sessionUserId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        Map<String, Object> sessionAttributes = new HashMap<>();
        if (sessionUserId != null) {
            sessionAttributes.put(JwtHandshakeInterceptor.USER_ID_SESSION_ATTRIBUTE, sessionUserId.toString());
        }
        accessor.setSessionAttributes(sessionAttributes);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void allowsSubscriptionForThePortfolioOwner() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(
            new PortfolioProjectionEntity(portfolioId, accountId, "P", "DEMO", BigDecimal.TEN, 1L, Instant.now())));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
            new AccountEntity(accountId, userId, AccountEntity.AccountType.DEMO, "Account")));

        Message<byte[]> message = subscribeMessage("/topic/portfolios/" + portfolioId + "/risk-alerts", userId);

        assertThat(interceptor.preSend(message, null)).isNotNull();
    }

    @Test
    void blocksSubscriptionForANonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(
            new PortfolioProjectionEntity(portfolioId, accountId, "P", "DEMO", BigDecimal.TEN, 1L, Instant.now())));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(
            new AccountEntity(accountId, ownerId, AccountEntity.AccountType.DEMO, "Account")));

        Message<byte[]> message = subscribeMessage("/topic/portfolios/" + portfolioId + "/risk-alerts", intruderId);

        assertThat(interceptor.preSend(message, null)).isNull();
    }

    @Test
    void blocksSubscriptionForAnUnknownPortfolio() {
        UUID portfolioId = UUID.randomUUID();
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        Message<byte[]> message = subscribeMessage("/topic/portfolios/" + portfolioId + "/orders", UUID.randomUUID());

        assertThat(interceptor.preSend(message, null)).isNull();
    }

    @Test
    void blocksSubscriptionWithNoAuthenticatedSession() {
        Message<byte[]> message = subscribeMessage("/topic/portfolios/" + UUID.randomUUID() + "/risk-alerts", null);

        assertThat(interceptor.preSend(message, null)).isNull();
    }

    @Test
    void allowsNonPortfolioTopicsThroughWithoutAnyOwnershipCheck() {
        Message<byte[]> message = subscribeMessage("/topic/prices/BTCUSDT", null);

        assertThat(interceptor.preSend(message, null)).isNotNull();
    }
}
