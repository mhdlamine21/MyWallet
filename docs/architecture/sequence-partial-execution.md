# Sequence - Partial Order Execution

Shows how a simulated external execution feed produces `TradeExecution` records, and how
the Order aggregate decides - purely from its own state - whether this fill completes the
order (`OrderFilled`) or leaves it partially filled (`OrderPartiallyFilled`).

```mermaid
sequenceDiagram
    participant Feed as SimulatedExecutionFeed
    participant App as RecordExecutionService
    participant Idem as IdempotencyGuard (Redis)
    participant Store as EventStoreRepository
    participant Order as Order (aggregate)
    participant Bus as RabbitMQ
    participant Pos as PositionProjector
    participant Risk as RiskEngine

    Feed->>App: ExecutionReceived(orderId, qty, price, fees, externalReference)
    App->>Idem: seen(externalReference)?
    alt already processed
        Idem-->>App: yes - duplicate
        App-->>Feed: ack (no-op, idempotent)
    else new execution
        Idem-->>App: no
        App->>Store: loadHistory(orderId)
        Store-->>App: List~DomainEvent~
        App->>Order: Order.reconstruct(id, history)
        App->>Order: recordPartialFill(qty, price, fees, ...)
        alt remaining quantity becomes zero
            Order-->>App: raises OrderFilled
        else quantity remains
            Order-->>App: raises OrderPartiallyFilled
        end
        App->>Store: append(event, expectedVersion = order.version)
        Note over Store: Optimistic lock: rejects if another<br/>execution was recorded concurrently
        App->>Idem: markSeen(externalReference)
        App->>Bus: publish event

        Bus->>Pos: update position (quantity, avg. acquisition price)
        Bus->>Risk: re-evaluate exposure after fill
    end
```
