-- V4__strategies.sql
-- Strategy is a plain versioned entity (optimistic locking via `version`), not
-- event-sourced - see docs/architecture/domain-model.md's note on why: its lifecycle
-- (DRAFT -> ACTIVE -> SUSPENDED -> DEACTIVATED) doesn't need full event replay.

CREATE TABLE strategies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                TEXT NOT NULL,
    description         TEXT,
    owner_id            UUID NOT NULL REFERENCES users(id),
    portfolio_id        UUID NOT NULL REFERENCES portfolio_projections(id),
    status              TEXT NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, SUSPENDED, DEACTIVATED
    mode                TEXT NOT NULL,                  -- BACKTEST, PAPER, LIVE_SIMULATION
    risk_level          TEXT NOT NULL DEFAULT 'MEDIUM',
    maximum_capital     NUMERIC(20,8),
    maximum_loss        NUMERIC(20,8),
    rule_expression     TEXT NOT NULL,                  -- parsed & validated by RuleParser at save time
    version             INT NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_strategies_owner ON strategies(owner_id);
CREATE INDEX idx_strategies_portfolio ON strategies(portfolio_id);
CREATE INDEX idx_strategies_status ON strategies(status);
