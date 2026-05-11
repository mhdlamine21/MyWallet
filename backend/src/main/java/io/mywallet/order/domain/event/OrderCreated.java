package io.mywallet.order.domain.event;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;
import io.mywallet.order.domain.model.OrderSide;
import io.mywallet.order.domain.model.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreated(
    EventMetadata metadata,
    UUID aggregateId,
    UUID portfolioId,
    UUID assetId,
    OrderType orderType,
    OrderSide side,
    BigDecimal quantity,
    BigDecimal limitPrice // nullable for MARKET orders
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "Order";
    }

    @Override
    public String eventType() {
        return "OrderCreated";
    }

    @Override
    public int eventVersion() {
        return 1;
    }
}
