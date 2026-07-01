-- V6__backtests.sql

CREATE TABLE backtests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy_id         UUID NOT NULL REFERENCES strategies(id),
    asset_id            UUID NOT NULL REFERENCES assets(id),
    initial_capital     NUMERIC(20,8) NOT NULL,
    period_start        TIMESTAMPTZ NOT NULL,
    period_end          TIMESTAMPTZ NOT NULL,
    fee_rate            NUMERIC(10,6) NOT NULL DEFAULT 0,
    slippage_rate       NUMERIC(10,6) NOT NULL DEFAULT 0,
    monte_carlo_seed    BIGINT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE backtest_results (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backtest_id                 UUID NOT NULL REFERENCES backtests(id) ON DELETE CASCADE,
    final_capital               NUMERIC(20,8) NOT NULL,
    total_return                NUMERIC(20,8) NOT NULL,
    annualized_return           NUMERIC(20,8) NOT NULL,
    number_of_trades            INT NOT NULL,
    win_rate                    NUMERIC(10,6) NOT NULL,
    average_gain                NUMERIC(20,8) NOT NULL,
    average_loss                NUMERIC(20,8) NOT NULL,
    max_drawdown                NUMERIC(10,6) NOT NULL,
    sharpe_ratio                NUMERIC(10,4) NOT NULL,
    sortino_ratio                NUMERIC(10,4) NOT NULL,
    equity_curve                JSONB NOT NULL,
    buy_and_hold_equity_curve   JSONB NOT NULL,
    monte_carlo_p5              JSONB NOT NULL,
    monte_carlo_p50             JSONB NOT NULL,
    monte_carlo_p95             JSONB NOT NULL,
    trades                      JSONB NOT NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_backtest_results_backtest ON backtest_results(backtest_id);
