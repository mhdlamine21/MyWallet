package io.mywallet.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * The metadata every {@link DomainEvent} carries, regardless of which aggregate or module
 * it belongs to. Extracted as its own value object so that the ~20 event types across
 * order/portfolio/strategy/risk/... declare it once instead of repeating five identical
 * fields each - see ADR-0004.
 */
public record EventMetadata(
    UUID eventId,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    UUID actorId
) {

    /**
     * Convenience factory for the common case: a brand-new event, timestamped now, tied to
     * a workflow via {@code correlationId}, optionally caused by a prior event/command via
     * {@code causationId} (nullable - null for the first event in a chain), attributed to
     * {@code actorId} (nullable for system-generated events, e.g. the market data generator).
     */
    public static EventMetadata create(UUID correlationId, UUID causationId, UUID actorId) {
        return new EventMetadata(UUID.randomUUID(), Instant.now(), correlationId, causationId, actorId);
    }
}
