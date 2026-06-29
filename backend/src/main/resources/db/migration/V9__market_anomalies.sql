-- V9__market_anomalies.sql

CREATE TABLE market_anomalies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id        UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    price           NUMERIC(20,8) NOT NULL,
    z_score         NUMERIC(10,4) NOT NULL,
    explanation     TEXT NOT NULL,
    detected_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_market_anomalies_asset ON market_anomalies(asset_id);
CREATE INDEX idx_market_anomalies_detected_at ON market_anomalies(detected_at DESC);
