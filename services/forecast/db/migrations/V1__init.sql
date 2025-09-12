-- Forecast DB schema (PoC)

CREATE TABLE IF NOT EXISTS model_versions (
  model_version_id      BIGSERIAL PRIMARY KEY,
  model_name            TEXT NOT NULL,
  version_label         TEXT NOT NULL,
  algorithm             TEXT NOT NULL,
  trained_at            timestamptz NOT NULL DEFAULT now(),
  artifact_path         TEXT NOT NULL,
  training_window_days  INTEGER NOT NULL,
  hyperparams           JSONB NOT NULL DEFAULT '{}'::jsonb,
  metrics               JSONB NOT NULL DEFAULT '{}'::jsonb,
  is_active             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS training_runs (
  training_run_id   BIGSERIAL PRIMARY KEY,
  model_version_id  BIGINT REFERENCES model_versions(model_version_id),
  scope             TEXT NOT NULL CHECK (scope IN ('global','product')),
  product_id        BIGINT,
  started_at        timestamptz NOT NULL DEFAULT now(),
  finished_at       timestamptz,
  status            TEXT NOT NULL CHECK (status IN ('queued','running','succeeded','failed')),
  params            JSONB NOT NULL DEFAULT '{}'::jsonb,
  metrics           JSONB NOT NULL DEFAULT '{}'::jsonb,
  error_message     TEXT
);

CREATE TABLE IF NOT EXISTS forecasts (
  forecast_id       BIGSERIAL PRIMARY KEY,
  requested_at      timestamptz NOT NULL DEFAULT now(),
  as_of_date        DATE NOT NULL,
  horizon_days      INTEGER NOT NULL,
  model_version_id  BIGINT REFERENCES model_versions(model_version_id),
  request_hash      TEXT UNIQUE,
  created_by        TEXT,
  note              TEXT
);

CREATE TABLE IF NOT EXISTS forecast_items (
  forecast_item_id  BIGSERIAL PRIMARY KEY,
  forecast_id       BIGINT REFERENCES forecasts(forecast_id) ON DELETE CASCADE,
  product_id        BIGINT NOT NULL,
  forecast_date     DATE NOT NULL,
  yhat              DOUBLE PRECISION NOT NULL,
  confidence        JSONB NOT NULL,
  lower_bound       DOUBLE PRECISION,
  upper_bound       DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_forecast_items_product_date ON forecast_items (product_id, forecast_date);
CREATE INDEX IF NOT EXISTS idx_forecasts_asof_model ON forecasts (as_of_date, model_version_id);

-- Accuracy tracking per product/day (compare prediction vs actual)
CREATE TABLE IF NOT EXISTS forecast_accuracy (
  accuracy_id       BIGSERIAL PRIMARY KEY,
  product_id        BIGINT NOT NULL,
  forecast_date     DATE NOT NULL,
  horizon_days      INTEGER NOT NULL,
  predicted_value   DOUBLE PRECISION NOT NULL,
  actual_value      DOUBLE PRECISION,
  error_pct         DOUBLE PRECISION,
  model_version_id  BIGINT REFERENCES model_versions(model_version_id),
  calculated_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_forecast_accuracy_product_date ON forecast_accuracy (product_id, forecast_date);
