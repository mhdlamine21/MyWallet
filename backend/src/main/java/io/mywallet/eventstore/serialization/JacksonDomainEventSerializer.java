package io.mywallet.eventstore.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mywallet.common.domain.DomainEvent;
import org.springframework.stereotype.Component;

/**
 * Serializes/deserializes concrete {@link DomainEvent} records to/from the JSON stored in
 * {@code domain_events.payload}. Deliberately dumb: it has no idea which event types
 * exist - callers supply the target class (usually resolved via an
 * {@link EventTypeRegistry}) when deserializing.
 */
@Component
public class JacksonDomainEventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonDomainEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException(
                "Failed to serialize event %s for aggregate %s".formatted(event.eventType(), event.aggregateId()), e);
        }
    }

    public DomainEvent deserialize(String payload, Class<? extends DomainEvent> targetType) {
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (Exception e) {
            throw new EventSerializationException(
                "Failed to deserialize payload into %s".formatted(targetType.getSimpleName()), e);
        }
    }

    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
