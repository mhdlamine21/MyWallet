package io.mywallet.portfolio.infrastructure.persistence;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.eventstore.serialization.EventTypeRegistry;
import io.mywallet.portfolio.domain.event.PortfolioCreated;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PortfolioEventTypeRegistry implements EventTypeRegistry {

    private static final Map<String, Class<? extends DomainEvent>> TYPES = Map.of(
        "PortfolioCreated", PortfolioCreated.class
        // PortfolioEmergencyStopped and position-related events are added as later
        // phases (kill switch in Phase 7, position projection wiring in Phase 5) land.
    );

    @Override
    public Class<? extends DomainEvent> resolve(String eventType) {
        Class<? extends DomainEvent> type = TYPES.get(eventType);
        if (type == null) {
            throw new IllegalArgumentException("Unknown Portfolio event type: " + eventType);
        }
        return type;
    }

    @Override
    public boolean supports(String eventType) {
        return TYPES.containsKey(eventType);
    }
}
