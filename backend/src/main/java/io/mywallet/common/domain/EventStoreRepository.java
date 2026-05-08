package io.mywallet.common.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The seam between an event-sourced aggregate and however its event stream is actually
 * persisted. This interface lives in the domain package deliberately - it is a <em>port</em>
 * in the hexagonal sense: application services depend on this abstraction only, never on
 * Spring Data JPA or any other persistence technology directly.
 *
 * <p>One adapter exists initially (a JPA-backed implementation writing to the
 * {@code domain_event} table - see ADR-0003), but the interface is designed as a real seam
 * because {@code Order}, {@code Portfolio}, and potentially {@code Strategy} all need it,
 * and an in-memory test double is a second, equally real adapter used throughout the
 * application-service test suite.</p>
 *
 * @param <T> the aggregate type this repository handles (e.g. {@code Order})
 */
public interface EventStoreRepository<T extends AggregateRoot> {

    /**
     * Persists the aggregate's uncommitted events, atomically checking that no other
     * writer has advanced the stream past {@code expectedVersion} since the aggregate was
     * loaded.
     *
     * @throws io.mywallet.common.exception.OptimisticConcurrencyException if the stream's
     *         actual current version does not match {@code expectedVersion}
     */
    void append(T aggregate, long expectedVersion);

    /**
     * Loads and reconstructs an aggregate from its full event history.
     *
     * @param aggregateId       id of the aggregate to load
     * @param emptyAggregateFactory factory producing an empty aggregate shell to replay
     *                              events onto (e.g. {@code Order::reconstructShell}) -
     *                              passed in rather than hard-coded so this one interface
     *                              serves every aggregate type without a type registry
     * @return empty if no events exist for this id
     */
    Optional<T> load(UUID aggregateId, Function<UUID, T> emptyAggregateFactory);

    /** Full event history for an aggregate, in version order. */
    List<DomainEvent> loadHistory(UUID aggregateId);

    /**
     * Event history for an aggregate up to (and including) a point in time - powers
     * point-in-time reconstruction (e.g. {@code GET /api/portfolios/{id}?at=...}) and the
     * Mode Replay feature's time-windowed batches.
     */
    List<DomainEvent> loadHistory(UUID aggregateId, Instant until);
}
