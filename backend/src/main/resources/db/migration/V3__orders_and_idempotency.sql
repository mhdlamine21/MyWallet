-- V3__orders_and_idempotency.sql

-- Order - read projection, mirroring the Portfolio pattern from V2.
-- Source of truth remains domain_events (aggregate_type = 'Order').
-- status/filled_quantity here are denormalized, never written to directly
-- except by OrderProjector - see docs/architecture/db-schema.md.

CREATE TABLE order_projections (
    id                  UUID PRIMARY KEY,
    portfolio_id        UUID NOT NULL REFERENCES portfolio_projections(id),
    asset_id            UUID NOT NULL REFERENCES assets(id),
    order_type          TEXT NOT NULL,
    side                TEXT NOT NULL,
    quantity            NUMERIC(20,8) NOT NULL,
    limit_price         NUMERIC(20,8),
    filled_quantity     NUMERIC(20,8) NOT NULL DEFAULT 0,
    status              TEXT NOT NULL,
    version             BIGINT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_projections_portfolio ON order_projections(portfolio_id);
CREATE INDEX idx_order_projections_status ON order_projections(status);

-- Generic idempotency guard for asynchronous event projectors (position
-- updates, audit, notifications, ...). A projector checks/inserts its own
-- (event_id, projector_name) row before applying an event; RabbitMQ's
-- at-least-once delivery means the same message can arrive twice, and this
-- table is what makes a re-delivery a safe no-op. See test scenario
-- "Double réception du même événement" in the project's test plan.

CREATE TABLE processed_projector_events (
    event_id            UUID NOT NULL,
    projector_name       TEXT NOT NULL,
    processed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, projector_name)
);

-- Idempotency for the simulated execution feed: an execution's
-- external_reference must never be applied twice, even under concurrent
-- delivery. Distinct from processed_projector_events because this guards
-- the *write* into the Order aggregate itself (before any event even
-- exists), not a downstream projection of an already-persisted event.

CREATE TABLE execution_idempotency (
    external_reference   TEXT PRIMARY KEY,
    order_id             UUID NOT NULL,
    received_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
