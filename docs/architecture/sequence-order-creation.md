# Sequence - Order Creation

Shows the synchronous validation path (risk check must confirm immediately) followed by
the asynchronous side effects (position update, audit, notification) fired via RabbitMQ.

```mermaid
sequenceDiagram
    actor Investor
    participant API as OrderController
    participant App as CreateOrderService
    participant Risk as RiskEngine
    participant Order as Order (aggregate)
    participant Store as EventStoreRepository
    participant Bus as RabbitMQ
    participant Pos as PositionProjector
    participant Audit as AuditLogger
    participant WS as WebSocket

    Investor->>API: POST /api/orders
    API->>App: CreateOrderCommand
    App->>Risk: validate(command)
    Note over Risk: Synchronous - the order must not<br/>be accepted if a limit is breached
    alt risk check fails
        Risk-->>App: RiskLimitBreached
        App-->>API: 422 Unprocessable Entity
        API-->>Investor: rejection reason
    else risk check passes
        Risk-->>App: OK
        App->>Order: Order.create(...)
        Order-->>App: raises OrderCreated
        App->>Store: append(OrderCreated, expectedVersion)
        Store-->>App: persisted (optimistic lock OK)
        App-->>API: 201 Created (orderId)
        API-->>Investor: order accepted

        App->>Bus: publish OrderCreated
        Note over Bus: Asynchronous fan-out
        Bus->>Pos: OrderCreated
        Pos->>Pos: reserve funds / update projection
        Bus->>Audit: OrderCreated
        Audit->>Audit: append audit log entry
        Bus->>WS: OrderCreated
        WS-->>Investor: real-time order status update
    end
```
