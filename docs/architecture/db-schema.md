# MyWallet - Relational Schema (reference)

This is the design-time reference schema. The authoritative, versioned source of truth
once Phase 2 starts will be the Flyway migrations under
`backend/src/main/resources/db/migration/`.

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned_to
    ROLES ||--o{ ROLE_PERMISSIONS : grants
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : granted_by
    USERS ||--o{ ACCOUNTS : owns
    ACCOUNTS ||--o{ PORTFOLIOS : contains
    PORTFOLIOS ||--o{ PORTFOLIO_POSITIONS : holds
    ASSETS ||--o{ PORTFOLIO_POSITIONS : referenced_by
    ASSETS ||--o{ MARKET_PRICES : has
    PORTFOLIOS ||--o{ ORDERS : places
    ORDERS ||--o{ TRADE_EXECUTIONS : filled_by
    PORTFOLIOS ||--o{ STRATEGIES : owns
    STRATEGIES ||--o{ STRATEGY_VERSIONS : versioned_as
    STRATEGY_VERSIONS ||--o{ TRADING_RULES : composed_of
    PORTFOLIOS ||--o{ RISK_LIMITS : constrained_by
    RISK_LIMITS ||--o{ RISK_ALERTS : breach_raises
    STRATEGIES ||--o{ BACKTESTS : backtested_via
    BACKTESTS ||--|| BACKTEST_RESULTS : produces
    USERS ||--o{ NOTIFICATIONS : receives
    USERS ||--o{ AUDIT_LOGS : performs

    USERS {
        uuid id PK
        text email UK
        text password_hash
        timestamptz created_at
        timestamptz updated_at
        uuid created_by
        uuid updated_by
    }

    ORDERS {
        uuid id PK
        uuid portfolio_id FK
        uuid asset_id FK
        text order_type
        text side
        numeric quantity
        numeric limit_price
        numeric filled_quantity
        text status
        bigint version
        timestamptz created_at
        timestamptz updated_at
    }

    TRADE_EXECUTIONS {
        uuid id PK
        uuid order_id FK
        numeric quantity
        numeric execution_price
        numeric fees
        timestamptz executed_at
        text external_reference UK
    }

    DOMAIN_EVENTS {
        uuid event_id PK
        uuid aggregate_id
        text aggregate_type
        text event_type
        bigint event_version
        jsonb payload
        timestamptz occurred_at
        uuid correlation_id
        uuid causation_id
        uuid actor_id
    }

    RISK_LIMITS {
        uuid id PK
        uuid portfolio_id FK
        text limit_type
        numeric threshold
    }

    RISK_ALERTS {
        uuid id PK
        uuid portfolio_id FK
        uuid risk_limit_id FK
        text level
        int risk_score
        text explanation
        timestamptz raised_at
    }
```

## Key design notes

- **`domain_events` is append-only and is never deleted, ever** - this is a hard rule from
  the project brief ("ne jamais supprimer les événements d'audit"). No `DELETE` grant on
  this table for any application role, enforced both at the ORM level and via a Postgres
  `REVOKE`.
- **Index plan:** `(aggregate_id, event_version)` unique composite index on `domain_events`
  - this is the index the replay query in `sequence-event-replay.md` relies on. A secondary
  index on `(occurred_at)` supports the point-in-time reconstruction query
  (`occurred_at <= :at`).
- **`orders.version`** is the JPA `@Version` optimistic-lock column - separate from
  `domain_events.event_version`, which is per-event. `orders.filled_quantity` and
  `orders.status` are **denormalized read projections** derived from events; the source of
  truth remains `domain_events`. This is documented explicitly so nobody "fixes" the
  projection table directly during a hotfix and silently diverges from the event log.
- **`trade_executions.external_reference`** carries a unique constraint - this is the
  idempotency key used by `sequence-partial-execution.md` to reject duplicate execution
  events from the simulated feed.
