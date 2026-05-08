package io.mywallet.common.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Base class for event-sourced aggregates (e.g. Order, Portfolio, Strategy).
 *
 * <p>An aggregate's current state is never persisted directly. It is always derived by
 * replaying the sequence of {@link DomainEvent}s that occurred to it. Callers apply new
 * events through {@link #raise(DomainEvent)}, which both mutates in-memory state (via
 * {@link #apply(DomainEvent)}) and records the event as "uncommitted" so the application
 * layer can persist it to the event store.</p>
 *
 * <p>Deliberately framework-free: no Spring, no JPA annotations. This class belongs to the
 * domain layer and must be usable in pure unit tests with zero infrastructure.</p>
 */
public abstract class AggregateRoot {

    private final UUID id;
    private long version = 0L;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected AggregateRoot(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    /** Optimistic-locking version, incremented once per applied event. */
    public long getVersion() {
        return version;
    }

    /**
     * Raises a new domain event: applies it to mutate state, and queues it to be
     * persisted by the application layer.
     */
    protected void raise(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
        version++;
    }

    /**
     * Rebuilds aggregate state by replaying a full history of past events, without
     * re-queuing them as uncommitted. Used when reconstructing an aggregate from the
     * event store (e.g. to reconstruct an Order or a Portfolio at a given point in time).
     */
    public void loadFromHistory(List<DomainEvent> history) {
        for (DomainEvent event : history) {
            apply(event);
            version++;
        }
    }

    /** Mutates in-memory state for a single event. Implemented by each concrete aggregate. */
    protected abstract void apply(DomainEvent event);

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
