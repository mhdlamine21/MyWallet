-- V5__risk.sql

CREATE TABLE risk_limits (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id    UUID NOT NULL REFERENCES portfolio_projections(id) ON DELETE CASCADE,
    limit_type      TEXT NOT NULL, -- MAX_ORDER_VALUE, MAX_EXPOSURE_PER_ASSET, MAX_ORDERS_PER_DAY
    threshold       NUMERIC(20,8) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (portfolio_id, limit_type)
);

CREATE TABLE risk_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id    UUID NOT NULL REFERENCES portfolio_projections(id) ON DELETE CASCADE,
    limit_type      TEXT NOT NULL,
    level           TEXT NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    risk_score      INT NOT NULL,  -- 0-100
    explanation     TEXT NOT NULL,
    raised_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_risk_alerts_portfolio ON risk_alerts(portfolio_id);
CREATE INDEX idx_risk_alerts_raised_at ON risk_alerts(raised_at DESC);

-- Single-row global kill switch - a real circuit breaker: ADMIN_KILL_SWITCH permission
-- (seeded in V1) gates who can flip it. When enabled, no new orders are accepted
-- anywhere in the system, regardless of portfolio or user.
CREATE TABLE kill_switch (
    id              TEXT PRIMARY KEY DEFAULT 'GLOBAL',
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    activated_by    UUID,
    activated_at    TIMESTAMPTZ,
    reason          TEXT,
    CONSTRAINT kill_switch_singleton CHECK (id = 'GLOBAL')
);
INSERT INTO kill_switch (id, enabled) VALUES ('GLOBAL', FALSE);
