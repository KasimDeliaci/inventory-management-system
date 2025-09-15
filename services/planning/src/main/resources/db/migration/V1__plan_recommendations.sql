-- Planning Service – Initial schema (PoC)
-- Creates a single table to persist LLM-based recommendations per forecast+product.
-- Postgres dialect.

CREATE TABLE IF NOT EXISTS plan_recommendations (
    plan_id           BIGSERIAL PRIMARY KEY,

    -- Link back to the forecast run that this recommendation is for (no FK across DBs)
    forecast_id       BIGINT NOT NULL,

    -- Product being planned for
    product_id        BIGINT NOT NULL,

    -- Forecast context (denormalized for quick filtering/UX)
    as_of_date        DATE   NOT NULL,
    horizon_days      INTEGER NOT NULL DEFAULT 7 CHECK (horizon_days IN (1, 7, 14)),

    -- Simple text payload: stores the raw response from LLM
    -- Can be JSON string, plain text, or any format the LLM returns
    response_json     TEXT   NOT NULL,

    -- Model metadata (for audit/demo)
    model             VARCHAR(50) NOT NULL DEFAULT 'gemma3:4b',

    -- Creation timestamp
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Idempotency for webhook/UI retries: only one recommendation per (forecast_id, product_id)
-- Note: Partial index guards when forecast_id is available.
CREATE UNIQUE INDEX IF NOT EXISTS ux_plan_recommendations_forecast_product
    ON plan_recommendations (forecast_id, product_id);

-- Useful filter for UI listings
CREATE INDEX IF NOT EXISTS ix_plan_recommendations_product_asof
    ON plan_recommendations (product_id, as_of_date DESC);

COMMENT ON TABLE plan_recommendations IS 'LLM-based replenishment recommendations keyed by forecast run and product';
COMMENT ON COLUMN plan_recommendations.forecast_id IS 'Optional: forecast run id from Forecast service (not FK across DBs)';
COMMENT ON COLUMN plan_recommendations.response_json IS 'Raw text response from LLM service (can be JSON, plain text, etc.)';