package io.mywallet.portfolio.domain.event;

import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;
import io.mywallet.portfolio.domain.model.PortfolioMode;

import java.math.BigDecimal;
import java.util.UUID;

public record PortfolioCreated(
    EventMetadata metadata,
    UUID aggregateId,
    UUID accountId,
    String name,
    PortfolioMode mode,
    BigDecimal initialCashBalance
) implements DomainEvent {

    @Override
    public String aggregateType() {
        return "Portfolio";
    }

    @Override
    public String eventType() {
        return "PortfolioCreated";
    }

    @Override
    public int eventVersion() {
        return 1;
    }
}
