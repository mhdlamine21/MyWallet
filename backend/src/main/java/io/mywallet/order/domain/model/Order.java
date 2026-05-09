package io.mywallet.order.domain.model;

import io.mywallet.common.domain.AggregateRoot;
import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;
import io.mywallet.order.domain.event.OrderCancelled;
import io.mywallet.order.domain.event.OrderCreated;
import io.mywallet.order.domain.event.OrderFilled;
import io.mywallet.order.domain.event.OrderPartiallyFilled;
import io.mywallet.order.domain.exception.InsufficientRemainingQuantityException;
import io.mywallet.order.domain.exception.InvalidOrderStateTransitionException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The Order aggregate - the central showcase of MyWallet's event-sourcing approach.
 *
 * =========================================================================================
 * NOTE D'APPRENTISSAGE ÉTUDIANT (Semaine 3 - Mon déclic Event Sourcing & Traçabilité Légale)
 * =========================================================================================
 * - Erreur initiale (Semaine 1) :
 *   J'avais bêtement codé un simple CRUD JPA avec `order.setStatus("FILLED")`.
 *   Problème soulevé par le prof : "Si un utilisateur conteste un ordre ou accuse la plateforme
 *   de manipulation, comment tu prouves chronologiquement chaque tick et exécution sans historique d'audit ?"
 * - Découverte & Solution :
 *   Après avoir lu le pattern Event Sourcing (Martin Fowler) et le modèle CQRS :
 *   1) Zéro setter direct : l'état est immuable de l'extérieur.
 *   2) Toute action métier lève un {@link DomainEvent} (OrderCreated, OrderFilled, etc.) via {@link #raise(DomainEvent)}.
 *   3) L'état interne n'est muté QUE dans la méthode {@link #apply(DomainEvent)}.
 *   4) Résultat magique : on peut rejouer n'importe quel ordre depuis le début (replay) et prouver
 *      l'intégrité de l'exécution à la seconde près.
 * =========================================================================================
 */
public class Order extends AggregateRoot {

    private UUID portfolioId;
    private UUID assetId;
    private OrderType orderType;
    private OrderSide side;
    private BigDecimal quantity;
    private BigDecimal limitPrice;
    private BigDecimal filledQuantity = BigDecimal.ZERO;
    private OrderStatus status;

    private Order(UUID id) {
        super(id);
    }

    /** Factory: creates a brand-new order and raises {@link OrderCreated}. */
    public static Order create(
        UUID portfolioId,
        UUID assetId,
        OrderType orderType,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal limitPrice,
        UUID correlationId,
        UUID actorId
    ) {
        Order order = new Order(UUID.randomUUID());
        order.raise(new OrderCreated(
            EventMetadata.create(correlationId, null, actorId),
            order.getId(),
            portfolioId,
            assetId,
            orderType,
            side,
            quantity,
            limitPrice
        ));
        return order;
    }

    /**
     * Reconstructs an order purely from its event history (used by the event store
     * repository). Takes a {@link List} explicitly - not {@code Iterable} - so that a
     * lazily-streamed history from the repository must be materialized by the caller
     * first; this keeps the "replay must be reproducible and total" invariant checkable
     * at compile time instead of risking a runtime {@code ClassCastException}.
     */
    public static Order reconstruct(UUID id, List<DomainEvent> history) {
        Order order = new Order(id);
        order.loadFromHistory(history);
        return order;
    }

    /**
     * Produces an empty shell (no state, no events applied) for a known aggregate id.
     * Used exclusively by {@code EventStoreRepository} implementations as the
     * {@code emptyAggregateFactory} passed to {@link io.mywallet.common.domain.EventStoreRepository#load},
     * which then calls {@link #loadFromHistory(List)} on it. Not meant to be called from
     * application services directly - use {@link #create} or {@link #reconstruct} instead.
     */
    public static Order emptyShell(UUID id) {
        return new Order(id);
    }

    public void recordPartialFill(UUID executionId, BigDecimal fillQuantity, BigDecimal executionPrice,
                                   BigDecimal fees, UUID correlationId, UUID causationId, UUID actorId) {
        requireTransitionAllowed(OrderStatus.PARTIALLY_FILLED);
        BigDecimal remaining = quantity.subtract(filledQuantity);
        if (fillQuantity.compareTo(remaining) > 0) {
            throw new InsufficientRemainingQuantityException(remaining, fillQuantity);
        }

        BigDecimal newRemaining = remaining.subtract(fillQuantity);
        EventMetadata metadata = EventMetadata.create(correlationId, causationId, actorId);
        if (newRemaining.signum() == 0) {
            raise(new OrderFilled(metadata, getId(), executionId, fillQuantity, executionPrice, fees));
        } else {
            raise(new OrderPartiallyFilled(metadata, getId(), executionId, fillQuantity, executionPrice, fees, newRemaining));
        }
    }

    public void cancel(String reason, UUID correlationId, UUID causationId, UUID actorId) {
        requireTransitionAllowed(OrderStatus.CANCELLED);
        raise(new OrderCancelled(EventMetadata.create(correlationId, causationId, actorId), getId(), reason));
    }

    private void requireTransitionAllowed(OrderStatus target) {
        if (status != null && !status.canTransitionTo(target)) {
            throw new InvalidOrderStateTransitionException(status, target);
        }
    }

    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case OrderCreated e -> {
                this.portfolioId = e.portfolioId();
                this.assetId = e.assetId();
                this.orderType = e.orderType();
                this.side = e.side();
                this.quantity = e.quantity();
                this.limitPrice = e.limitPrice();
                this.status = OrderStatus.CREATED;
            }
            case OrderPartiallyFilled e -> {
                this.filledQuantity = this.filledQuantity.add(e.filledQuantity());
                this.status = OrderStatus.PARTIALLY_FILLED;
            }
            case OrderFilled e -> {
                this.filledQuantity = this.filledQuantity.add(e.filledQuantity());
                this.status = OrderStatus.FILLED;
            }
            case OrderCancelled e -> this.status = OrderStatus.CANCELLED;
            default -> throw new IllegalStateException("Unhandled event type for Order: " + event.eventType());
        }
    }

    public OrderStatus status() {
        return status;
    }

    public BigDecimal remainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    public BigDecimal filledQuantity() {
        return filledQuantity;
    }

    public UUID portfolioId() {
        return portfolioId;
    }

    public UUID assetId() {
        return assetId;
    }
}
