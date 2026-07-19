# MyWallet - Domain Model

This diagram covers the core aggregates and their relationships. Value objects and pure
lookup entities (Role, Permission) are simplified for readability - see the relational
schema (`db-schema.md`) for full column-level detail.

```mermaid
classDiagram
    class User {
        +UUID id
        +String email
        +String passwordHash
        +Set~Role~ roles
        +Instant createdAt
        +Instant updatedAt
    }

    class Role {
        +UUID id
        +String name
        +Set~Permission~ permissions
    }

    class Permission {
        +UUID id
        +String code
    }

    class Account {
        +UUID id
        +UUID ownerId
        +AccountType type
        +Instant createdAt
    }

    class Portfolio {
        +UUID id
        +UUID accountId
        +String name
        +PortfolioMode mode
        +BigDecimal cashBalance
        +createOrder()
        +applyExecution()
        +reconstructAt(Instant)
    }

    class PortfolioPosition {
        +UUID id
        +UUID portfolioId
        +UUID assetId
        +BigDecimal quantity
        +BigDecimal averageAcquisitionPrice
    }

    class Asset {
        +UUID id
        +String symbol
        +AssetClass assetClass
        +String currency
    }

    class MarketPrice {
        +UUID id
        +UUID assetId
        +BigDecimal price
        +Instant observedAt
    }

    class Order {
        +UUID id
        +UUID portfolioId
        +UUID assetId
        +OrderType orderType
        +OrderSide side
        +BigDecimal quantity
        +BigDecimal limitPrice
        +BigDecimal filledQuantity
        +OrderStatus status
        +long version
        +create()
        +recordPartialFill()
        +cancel()
        -apply(DomainEvent)
    }

    class TradeExecution {
        +UUID id
        +UUID orderId
        +BigDecimal quantity
        +BigDecimal executionPrice
        +BigDecimal fees
        +Instant executedAt
        +String externalReference
    }

    class DomainEvent {
        <<interface>>
        +UUID eventId
        +UUID aggregateId
        +String aggregateType
        +String eventType
        +long eventVersion
        +String payload
        +Instant occurredAt
        +UUID correlationId
        +UUID causationId
        +UUID actorId
    }

    class Strategy {
        +UUID id
        +String name
        +UUID ownerId
        +UUID portfolioId
        +StrategyStatus status
        +StrategyMode mode
        +BigDecimal maximumCapital
        +BigDecimal maximumLoss
        +int version
        +activate()
        +suspend()
        +deactivate()
    }

    class StrategyVersion {
        +UUID id
        +UUID strategyId
        +int versionNumber
        +String ruleExpressionAst
    }

    class TradingRule {
        +UUID id
        +String expression
        +evaluate(MarketContext) RuleResult
    }

    class RiskLimit {
        +UUID id
        +UUID portfolioId
        +RiskLimitType type
        +BigDecimal threshold
    }

    class RiskAlert {
        +UUID id
        +UUID portfolioId
        +RiskLevel level
        +int riskScore
        +String explanation
        +Instant raisedAt
    }

    class Backtest {
        +UUID id
        +UUID strategyId
        +BigDecimal initialCapital
        +Instant periodStart
        +Instant periodEnd
        +run() BacktestResult
    }

    class BacktestResult {
        +UUID id
        +UUID backtestId
        +BigDecimal finalCapital
        +BigDecimal totalReturn
        +BigDecimal sharpeRatio
        +BigDecimal sortinoRatio
        +BigDecimal maxDrawdown
    }

    class Notification {
        +UUID id
        +UUID userId
        +String channel
        +String content
        +boolean read
    }

    class AuditLog {
        +UUID id
        +UUID actorId
        +String action
        +String targetType
        +UUID targetId
        +Instant occurredAt
    }

    class BrokerConnection {
        +UUID id
        +String providerName
        +String status
    }

    User "1" --> "*" Role : has
    Role "1" --> "*" Permission : grants
    User "1" --> "*" Account : owns
    Account "1" --> "*" Portfolio : contains
    Portfolio "1" --> "*" PortfolioPosition : holds
    PortfolioPosition "*" --> "1" Asset : references
    Asset "1" --> "*" MarketPrice : has price history
    Portfolio "1" --> "*" Order : places
    Order "1" --> "*" TradeExecution : filled by
    Order "1" --> "*" DomainEvent : sourced from
    Portfolio "1" --> "*" DomainEvent : sourced from
    Strategy "1" --> "*" StrategyVersion : versioned as
    StrategyVersion "1" --> "*" TradingRule : composed of
    Strategy "*" --> "1" Portfolio : trades on
    Portfolio "1" --> "*" RiskLimit : constrained by
    RiskLimit "1" --> "*" RiskAlert : breach raises
    Strategy "1" --> "*" Backtest : backtested via
    Backtest "1" --> "1" BacktestResult : produces
    User "1" --> "*" Notification : receives
    User "1" --> "*" AuditLog : performs actions logged as
    Portfolio "*" --> "*" BrokerConnection : reconciled against
```

## Notes on the design

- **Order and Portfolio are the two event-sourced aggregates.** Their current state is
  never stored as a plain column set - it is always derived by replaying `DomainEvent`
  rows filtered by `aggregateId`. See `sequence-order-creation.md` and
  `sequence-event-replay.md`.
- **Strategy is versioned explicitly** (`StrategyVersion`) rather than event-sourced, since
  its lifecycle (draft -> active -> suspended -> deactivated) is simpler and doesn't need full
  replay - a normal optimistic-locking `version` column is sufficient.
- **RiskLimit / RiskAlert** are deliberately modeled as first-class domain objects (not just
  configuration), because a `RiskLimitBreached` event needs to reference *which* limit was
  breached, by how much, for the alert's explanation to be meaningful
  ("Exposure to BTCUSDT is 42%, configured limit is 25%").
