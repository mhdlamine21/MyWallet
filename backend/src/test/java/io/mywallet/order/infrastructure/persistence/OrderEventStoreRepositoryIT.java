package io.mywallet.order.infrastructure.persistence;

import io.mywallet.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mywallet.common.exception.OptimisticConcurrencyException;
import io.mywallet.order.domain.model.Order;
import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.domain.model.OrderStatus;
import io.mywallet.order.domain.model.OrderType;

/**
 * Exercises the real contract of {@link OrderEventStoreRepository} against a real
 * PostgreSQL instance - this is what proves the append-then-replay round trip actually
 * works end to end (JSON serialization, the unique-index-backed optimistic lock, and the
 * event-type registry), not just in the pure in-memory {@code OrderTest}.
 */
class OrderEventStoreRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private OrderEventStoreRepository repository;

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void appendedOrderCanBeReloadedWithTheSameState() {
        Order order = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );

        repository.append(order, 0L);

        Optional<Order> reloaded = repository.load(order.getId(), Order::emptyShell);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(reloaded.get().remainingQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void partialFillsAcrossTwoAppendsReplayToTheCorrectFinalState() {
        Order order = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );
        repository.append(order, 0L);

        Order reloaded = repository.load(order.getId(), Order::emptyShell).orElseThrow();
        reloaded.recordPartialFill(UUID.randomUUID(), new BigDecimal("4"), new BigDecimal("101.50"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);
        repository.append(reloaded, 1L); // one event (OrderCreated) was already persisted

        Order finalState = repository.load(order.getId(), Order::emptyShell).orElseThrow();

        assertThat(finalState.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(finalState.remainingQuantity()).isEqualByComparingTo("6");
        assertThat(repository.loadHistory(order.getId())).hasSize(2);
    }

    @Test
    void concurrentAppendWithStaleExpectedVersionIsRejected() {
        Order order = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );
        repository.append(order, 0L);

        // Two concurrent "writers" both load the order at version 1 and each try to
        // record a fill - simulating the "exécution simultanée de deux ordres" test
        // scenario from the project brief.
        Order writerA = repository.load(order.getId(), Order::emptyShell).orElseThrow();
        Order writerB = repository.load(order.getId(), Order::emptyShell).orElseThrow();

        writerA.recordPartialFill(UUID.randomUUID(), new BigDecimal("4"), new BigDecimal("101.50"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);
        repository.append(writerA, 1L); // succeeds - stream was still at version 1

        writerB.recordPartialFill(UUID.randomUUID(), new BigDecimal("3"), new BigDecimal("102.00"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);

        assertThatThrownBy(() -> repository.append(writerB, 1L)) // stale - stream is now at version 2
            .isInstanceOf(OptimisticConcurrencyException.class);
    }

    @Test
    void loadingAnUnknownAggregateReturnsEmpty() {
        Optional<Order> result = repository.load(UUID.randomUUID(), Order::emptyShell);
        assertThat(result).isEmpty();
    }
}
