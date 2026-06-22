-- V8__strategy_execution.sql

-- Tracks whether a strategy's automatic execution currently considers itself "long"
-- (holding a position) or "flat" - the scheduler only acts on a *transition*, not on
-- every tick where the signal happens to still be true/false, mirroring BacktestEngine's
-- own flat/long logic so live behavior matches what a backtest of the same rule predicts.
CREATE TABLE strategy_execution_states (
    strategy_id     UUID PRIMARY KEY REFERENCES strategies(id) ON DELETE CASCADE,
    currently_long  BOOLEAN NOT NULL DEFAULT FALSE,
    last_evaluated_at TIMESTAMPTZ
);

-- Captures the portfolio's value at the moment a strategy is activated, so the live
-- leaderboard can report "return since activation" rather than the portfolio's all-time
-- return (which could include manual trades or a prior strategy's activity).
CREATE TABLE strategy_performance_baselines (
    strategy_id         UUID PRIMARY KEY REFERENCES strategies(id) ON DELETE CASCADE,
    portfolio_id        UUID NOT NULL REFERENCES portfolio_projections(id),
    baseline_value      NUMERIC(20,8) NOT NULL,
    activated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
