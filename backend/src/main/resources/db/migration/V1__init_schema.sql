-- V1__init_schema.sql
-- Base schema: identity (users/roles/permissions), accounts, and the event store.
-- Order/Portfolio/Strategy tables land in later migrations as their phases are built.

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid()

-- Identity & access

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL UNIQUE, -- INVESTOR, TRADER, ANALYST, ADMIN, AUDITOR
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            TEXT NOT NULL UNIQUE, -- e.g. "ORDER_CREATE", "STRATEGY_ACTIVATE", "AUDIT_READ"
    description     TEXT
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens, stored hashed, to support rotation and revocation (Phase 3).
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT NOT NULL UNIQUE,
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by_id  UUID REFERENCES refresh_tokens(id)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- Accounts

CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_type    TEXT NOT NULL, -- REAL, SIMULATED, DEMO
    display_name    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID
);
CREATE INDEX idx_accounts_owner ON accounts(owner_id);

-- Event store - the source of truth for every event-sourced aggregate
-- (Order, Portfolio, and potentially Strategy). Append-only: no application
-- role is granted DELETE on this table (see grants below and ADR-0003).

CREATE TABLE domain_events (
    event_id        UUID PRIMARY KEY,
    aggregate_id    UUID NOT NULL,
    aggregate_type  TEXT NOT NULL,
    event_type      TEXT NOT NULL,
    event_version   BIGINT NOT NULL, -- position of this event within the aggregate's stream (1, 2, 3, ...)
    schema_version  INT NOT NULL DEFAULT 1, -- payload schema version for this event TYPE, for safe evolution
    payload         JSONB NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    correlation_id  UUID NOT NULL,
    causation_id    UUID,
    actor_id        UUID
);

-- Enforces optimistic concurrency: two writers cannot both append version N
-- for the same aggregate. Also the index the replay query relies on.
CREATE UNIQUE INDEX ux_domain_events_aggregate_version
    ON domain_events(aggregate_id, event_version);

-- Supports point-in-time reconstruction (occurred_at <= :at).
CREATE INDEX idx_domain_events_aggregate_occurred_at
    ON domain_events(aggregate_id, occurred_at);

-- Supports "give me everything that happened in this request" tracing.
CREATE INDEX idx_domain_events_correlation ON domain_events(correlation_id);

-- Seed data: roles + permissions
-- (User accounts themselves are seeded by DataSeeder in Phase 10, not here -
-- Flyway migrations should not contain environment-specific demo data.)

INSERT INTO roles (name) VALUES
    ('INVESTOR'), ('TRADER'), ('ANALYST'), ('ADMIN'), ('AUDITOR');

INSERT INTO permissions (code, description) VALUES
    ('PORTFOLIO_READ',      'View portfolios and positions'),
    ('ORDER_CREATE',        'Create buy/sell orders'),
    ('ORDER_CANCEL',        'Cancel own orders'),
    ('STRATEGY_MANAGE',     'Create, edit, activate, suspend strategies'),
    ('BACKTEST_RUN',        'Run backtests'),
    ('RISK_ALERTS_READ',    'View risk alerts'),
    ('AUDIT_READ',          'View audit logs (read-only)'),
    ('ADMIN_USERS_MANAGE',  'Manage users and role assignments'),
    ('ADMIN_KILL_SWITCH',   'Trigger the global emergency stop'),
    ('ADMIN_RECONCILIATION_RUN', 'Trigger a reconciliation run');
