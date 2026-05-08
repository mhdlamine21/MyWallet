package io.mywallet.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker contract for every domain event in the system (OrderCreated, OrderFilled,
 * RiskLimitBreached, StrategyActivated, ...).
 *
 * <p>Concrete events implement {@link #aggregateId()}, {@link #aggregateType()},
 * {@link #eventType()}, {@link #eventVersion()}, and {@link #metadata()} - the common
 * fields ({@code eventId}, {@code occurredAt}, {@code correlationId}, {@code causationId},
 * {@code actorId}) are then available "for free" via the default methods below, without
 * every event record having to declare them individually. See
 * {@code docs/architecture/adr/0004-event-metadata-value-object.md}.</p>
 */
public interface DomainEvent {

    /** Id of the aggregate this event belongs to (e.g. the Order id). */
    UUID aggregateId();

    /** Discriminator for the aggregate type, e.g. "Order", "Portfolio", "Strategy". */
    String aggregateType();

    /** Discriminator for the event type, e.g. "OrderCreated", "OrderFilled". */
    String eventType();

    /** Schema version of this event's payload, to support safe evolution over time. */
    int eventVersion();

    /** Common metadata (eventId, occurredAt, correlationId, causationId, actorId). */
    EventMetadata metadata();

    default UUID eventId() {
        return metadata().eventId();
    }

    default Instant occurredAt() {
        return metadata().occurredAt();
    }

    /** Ties this event to the request/workflow that produced it, for end-to-end tracing. */
    default UUID correlationId() {
        return metadata().correlationId();
    }

    /** Id of the event or command that caused this event, for causal chains. Nullable. */
    default UUID causationId() {
        return metadata().causationId();
    }

    /** Id of the user or system actor that triggered this event. Nullable for system-generated events. */
    default UUID actorId() {
        return metadata().actorId();
    }
}
