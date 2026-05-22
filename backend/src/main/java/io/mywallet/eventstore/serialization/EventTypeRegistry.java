package io.mywallet.eventstore.serialization;

import io.mywallet.common.domain.DomainEvent;

/**
 * Resolves a persisted {@code event_type} string (e.g. "OrderCreated") back to the
 * concrete Java record implementing {@link DomainEvent}, so the JSON payload can be
 * deserialized to the right type. One implementation per event-sourced module (Order
 * today; Portfolio and Strategy add their own in later phases) - kept separate rather
 * than one giant registry so each module owns its own event vocabulary.
 */
public interface EventTypeRegistry {

    Class<? extends DomainEvent> resolve(String eventType);

    /** True if this registry knows how to resolve the given event type. */
    boolean supports(String eventType);
}
