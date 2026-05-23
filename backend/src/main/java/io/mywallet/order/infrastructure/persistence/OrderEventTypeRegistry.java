package io.mywallet.order.infrastructure.persistence;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.eventstore.serialization.EventTypeRegistry;
import io.mywallet.order.domain.event.OrderCancelled;
import io.mywallet.order.domain.event.OrderCreated;
import io.mywallet.order.domain.event.OrderFilled;
import io.mywallet.order.domain.event.OrderPartiallyFilled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderEventTypeRegistry implements EventTypeRegistry {

    private static final Map<String, Class<? extends DomainEvent>> TYPES = Map.of(
        "OrderCreated", OrderCreated.class,
        "OrderPartiallyFilled", OrderPartiallyFilled.class,
        "OrderFilled", OrderFilled.class,
        "OrderCancelled", OrderCancelled.class
    );

    @Override
    public Class<? extends DomainEvent> resolve(String eventType) {
        Class<? extends DomainEvent> type = TYPES.get(eventType);
        if (type == null) {
            throw new IllegalArgumentException("Unknown Order event type: " + eventType);
        }
        return type;
    }

    @Override
    public boolean supports(String eventType) {
        return TYPES.containsKey(eventType);
    }
}
