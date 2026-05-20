-- V2__portfolios_assets.sql

-- Assets & simulated market data

CREATE TABLE assets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol          TEXT NOT NULL UNIQUE,
    asset_class     TEXT NOT NULL, -- STOCK, CRYPTO, CURRENCY, BOND, FUND, COMMODITY
    currency        TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    -- GBM simulation parameters, per asset (see MarketDataGeneratorService)
    initial_price   NUMERIC(20,8) NOT NULL,
    drift            NUMERIC(10,6) NOT NULL DEFAULT 0.00,   -- annualized expected return (mu)
    volatility       NUMERIC(10,6) NOT NULL DEFAULT 0.20,   -- annualized volatility (sigma)
    random_seed      BIGINT NOT NULL,                        -- deterministic replay for tests/backtests
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE market_prices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    price           NUMERIC(20,8) NOT NULL,
    observed_at     TIMESTAMPTZ NOT NULL
);
-- Supports "latest price for asset" and time-range queries (charts, backtests) efficiently.
CREATE INDEX idx_market_prices_asset_time ON market_prices(asset_id, observed_at DESC);

-- Portfolios - event-sourced, like Order. Current state is a projection over
-- domain_events (aggregate_type = 'Portfolio'), never written to directly
-- except by the projector. See docs/architecture/adr/0003 and Order's pattern.

-- Read projection only - the source of truth is domain_events. Kept in sync by
-- PortfolioProjector (application layer) reacting to PortfolioCreated and,
-- from Phase 5 onward, to Order fill events.
CREATE TABLE portfolio_projections (
    id              UUID PRIMARY KEY,
    account_id      UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    mode            TEXT NOT NULL, -- REAL, SIMULATED, DEMO
    cash_balance    NUMERIC(20,8) NOT NULL,
    version         BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_portfolio_projections_account ON portfolio_projections(account_id);

CREATE TABLE portfolio_positions (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id                UUID NOT NULL REFERENCES portfolio_projections(id) ON DELETE CASCADE,
    asset_id                    UUID NOT NULL REFERENCES assets(id),
    quantity                    NUMERIC(20,8) NOT NULL DEFAULT 0,
    average_acquisition_price   NUMERIC(20,8) NOT NULL DEFAULT 0,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (portfolio_id, asset_id)
);
CREATE INDEX idx_portfolio_positions_portfolio ON portfolio_positions(portfolio_id);
