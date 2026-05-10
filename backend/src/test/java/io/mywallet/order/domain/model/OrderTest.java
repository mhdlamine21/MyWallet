package io.mywallet.order.domain.model;

import io.mywallet.order.domain.exception.InvalidOrderStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure domain unit tests - no Spring context, no database. These run in milliseconds
 * and are the first line of defense for the event-sourcing correctness guarantee.
 */
class OrderTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final UUID ASSET_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Test
    void createsAnOrderInCreatedState() {
        Order order = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );

        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.remainingQuantity()).isEqualByComparingTo("10");
        assertThat(order.getUncommittedEvents()).hasSize(1);
    }

    @Test
    void partialFillReducesRemainingQuantityAndKeepsOrderOpen() {
        Order order = anOrderOfQuantity("10");

        order.recordPartialFill(UUID.randomUUID(), new BigDecimal("4"), new BigDecimal("101.50"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);

        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.remainingQuantity()).isEqualByComparingTo("6");
    }

    @Test
    void fillingTheRemainingQuantityMarksOrderAsFilled() {
        Order order = anOrderOfQuantity("10");

        order.recordPartialFill(UUID.randomUUID(), new BigDecimal("4"), new BigDecimal("101.50"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);
        order.recordPartialFill(UUID.randomUUID(), new BigDecimal("6"), new BigDecimal("102.00"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);

        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.remainingQuantity()).isEqualByComparingTo("0");
    }

    @Test
    void cannotFillMoreThanRemainingQuantity() {
        Order order = anOrderOfQuantity("10");

        assertThatThrownBy(() -> order.recordPartialFill(
            UUID.randomUUID(), new BigDecimal("11"), new BigDecimal("100"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID
        )).isInstanceOf(io.mywallet.order.domain.exception.InsufficientRemainingQuantityException.class);
    }

    @Test
    void cannotCancelAnAlreadyFilledOrder() {
        Order order = anOrderOfQuantity("5");
        order.recordPartialFill(UUID.randomUUID(), new BigDecimal("5"), new BigDecimal("100"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);

        assertThatThrownBy(() -> order.cancel("changed my mind", UUID.randomUUID(), null, ACTOR_ID))
            .isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    /**
     * The central guarantee of event sourcing: replaying an order's full event history
     * from scratch must produce byte-for-byte the same state as the "live" aggregate that
     * originally raised those events.
     */
    @Test
    void reconstructingFromHistoryProducesTheSameStateAsTheLiveAggregate() {
        Order live = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );
        live.recordPartialFill(UUID.randomUUID(), new BigDecimal("4"), new BigDecimal("101.50"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);
        live.recordPartialFill(UUID.randomUUID(), new BigDecimal("6"), new BigDecimal("102.00"),
            BigDecimal.ZERO, UUID.randomUUID(), null, ACTOR_ID);

        var history = live.getUncommittedEvents();
        Order reconstructed = Order.reconstruct(live.getId(), history);

        assertThat(reconstructed.status()).isEqualTo(live.status());
        assertThat(reconstructed.remainingQuantity()).isEqualByComparingTo(live.remainingQuantity());
        assertThat(reconstructed.getVersion()).isEqualTo(live.getVersion());
    }

    private Order anOrderOfQuantity(String qty) {
        Order order = Order.create(
            PORTFOLIO_ID, ASSET_ID, OrderType.LIMIT, OrderSide.BUY,
            new BigDecimal(qty), new BigDecimal("100.00"),
            UUID.randomUUID(), ACTOR_ID
        );
        order.markEventsAsCommitted();
        return order;
    }
}
