package io.mywallet.order.domain.event;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;

import java.util.UUID;

public record OrderCancelled(
    EventMetadata metadata,
    UUID aggregateId,
    String reason
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "Order";
    }

    @Override
    public String eventType() {
        return "OrderCancelled";
    }

    @Override
    public int eventVersion() {
        return 1;
    }
}
