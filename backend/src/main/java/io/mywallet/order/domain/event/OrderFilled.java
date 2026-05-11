package io.mywallet.order.domain.event;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderFilled(
    EventMetadata metadata,
    UUID aggregateId,
    UUID executionId,
    BigDecimal filledQuantity,
    BigDecimal executionPrice,
    BigDecimal fees
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "Order";
    }

    @Override
    public String eventType() {
        return "OrderFilled";
    }

    @Override
    public int eventVersion() {
        return 1;
    }
}
