package io.mywallet.order.interfaces.rest.dto;

import io.mywallet.common.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderEventResponse(
    UUID eventId,
    String eventType,
    int schemaVersion,
    Instant occurredAt,
    UUID correlationId,
    UUID causationId,
    UUID actorId,
    Object payload
) {
    public static OrderEventResponse from(DomainEvent event) {
        return new OrderEventResponse(
            event.eventId(), event.eventType(), event.eventVersion(), event.occurredAt(),
            event.correlationId(), event.causationId(), event.actorId(), event
        );
    }
}
