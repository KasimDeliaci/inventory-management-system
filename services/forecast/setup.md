# Forecast Service — Setup and Operations

This guide explains the big picture, one‑time setup, how to run the service, how the data flows, the database tables, and the available APIs.

## Big Picture

- Services
  - Inventory Service (Spring Boot + Postgres): owns operational data. Exposes reporting endpoints that serve the training/forecast features: daily sales, promos, and day‑level offer stats.
  - Forecast Service (FastAPI + Python): pulls features from Inventory over HTTP, trains models, serves forecasts, and optionally persists results in its own DB.
- Data stores
  - `inventory_db`: operational schema + forecasting views (`inv_forecast` schema) that back the reporting APIs.
  - `forecast_db`: model registry (`model_versions`, `training_runs`) and forecast outputs (`forecasts`, `forecast_items`, `forecast_accuracy`).
  - Model artifacts: stored on the filesystem under `MODEL_DIR` (default: `services/forecast/models`).
- Models
  - Baseline MA7 (simple moving average) for quick fallback.
  - XGBoost three‑head model (`xgb_three`): separate heads for next‑day (h=1), 7‑day sum, and 14‑day sum. Uses known‑future exogenous features (promos, offer pressure, calendar) and historical lags/MAs. Honors `hyperparams` from the /train request with early stopping.
- Feature sources (via Inventory API)
  - Daily sales: `date, productId, salesUnits, offerActiveShare(0..1)`
  - Daily promo: `date, productId, promoPct(0..100)`
  - Day offer stats: `date, activeOffersCount, offerAvgPct(0..100), offerMaxPct(0..100)`

## One‑Time Setup

1) Ensure Inventory is up and its reporting endpoints return data (migrations V1–V8 applied, service running):
   - Sales: `GET /api/v1/reporting/product-day-sales?from=2024-01-01&to=2024-02-01&productId=1008`
   - Promo: `GET /api/v1/reporting/product-day-promo?from=2024-01-01&to=2024-02-01&productId=1008`
   - Offers: `GET /api/v1/reporting/day-offer-stats?from=2024-01-01&to=2024-02-01`

2) Create and migrate `forecast_db` (one‑time)
   - If using psql to a Dockerized Postgres, force TCP with `-h`:
     - `psql -h localhost -U <user> -d forecast_db -f services/forecast/db/migrations/V1__init.sql`
   - Or in pgAdmin: open a query window on `forecast_db` and execute the SQL file above.
   - Verify tables:
     - `psql -h localhost -U <user> -d forecast_db -c '\dt'`
     - You should see: `model_versions`, `training_runs`, `forecasts`, `forecast_items`, `forecast_accuracy`.

3) Configure environment variables for Forecast
   - `FORECAST_DB_DSN=postgresql://<user>:<pass>@localhost:5432/forecast_db`
   - `INVENTORY_BASE_URL=http://localhost:8000` (adjust host/port as needed)
   - `MODEL_DIR=./services/forecast/models` (optional; default is already this path)
   - zsh (temporary):
     - `export FORECAST_DB_DSN='postgresql://admin:admin@localhost:5432/forecast_db'`
     - `export INVENTORY_BASE_URL='http://localhost:8000'`
   - zsh (persistent): add to `~/.zshrc` then `source ~/.zshrc`
   - Ensure DSN is one line: `echo "$FORECAST_DB_DSN"` and test: `psql "$FORECAST_DB_DSN" -c '\dt'`

## Run the Forecast Service

1) Install dependencies
   - `pip install -r services/forecast/requirements.txt`

2) Start API (prefer single worker while training)
   - `uvicorn services.forecast.app.main:app --port 8100 --workers 1`
   - Root `/` redirects to `/docs`; health at `/health`.

3) Health check
   - `GET http://localhost:8100/health` → `{ "status": "ok" }`

## Typical Flow (Train → Activate → Forecast)

1) Train `xgb_three` (36 months lookback example)
   - `POST /train`
     - Body: `{ "algorithm": "xgb_three", "trainWindowDays": 1095, "hyperparams": { /* optional xgb params */ }, "autoActivate": true }`
   - What happens:
     - Forecast pulls features from Inventory for the window, builds datasets for h1/y7/y14, trains three models with early stopping, computes conformal prediction intervals, saves artifacts under `MODEL_DIR/xgb_three/<version>/`, and inserts a `model_versions` row with metrics.
     - The new model auto‑activates by default (set `autoActivate:false` to skip).

1b) One‑off tuning (optional)
   - `POST /train/tune?trials=20&trainWindowDays=1095&seed=42&trainBest=true`
   - Runs a random search over xgb params and returns bestHyperparams/metrics. If `trainBest=true`, trains and auto‑activates the best model.
   - For large trials, prefer running outside peak hours; this endpoint is synchronous.

2) Inspect and activate the trained model
   - `GET /train/models` → list versions with metrics
   - `POST /train/activate/{modelVersionId}` → sets this version active (others inactive)

3) Request forecasts (requires DB persistence)
   - `POST /forecast`
     - Examples:
       - Next day: `{ "productIds": [1008,1009], "horizonDays": 1, "returnDaily": true }`
       - 7 days: `{ "productIds": [1008,1009], "horizonDays": 7, "returnDaily": true }`
       - 14 days: `{ "productIds": [1008,1009], "horizonDays": 14, "returnDaily": true }`
   - Response includes non‑null top‑level `forecastId`, per‑product `sum`, optional `daily[]`, `predictionInterval` (sum), and a volatility‑based `confidence` object. Results are written into `forecasts` + `forecast_items`.

4) Verify persistence (pgAdmin / psql)
   - `SELECT * FROM model_versions ORDER BY trained_at DESC LIMIT 5;`
   - `SELECT * FROM forecasts ORDER BY forecast_id DESC LIMIT 5;`
   - `SELECT * FROM forecast_items WHERE forecast_id=<ID> ORDER BY forecast_date;`

## Repository Map (Forecast)

- `services/forecast/app/main.py` — FastAPI app factory
- `services/forecast/app/routers/train.py` — Train endpoints (queue background job, registry ops)
  - Includes `/train/tune` for one‑off random search and optional immediate training
- `services/forecast/app/routers/forecast.py` — Forecast endpoint (uses active model or MA7 fallback)
- `services/forecast/app/clients/inventory.py` — HTTP client for Inventory reporting APIs
- `services/forecast/app/features/` — Feature builders
  - `calendar.py` — TR holidays + Ramadan/Eids + special days
  - `assembler.py` — Join sales, promos, calendar; add lags/MAs
  - `windows.py` — Horizon window aggregates + daily weighting allocator
- `services/forecast/app/models/` — Models
  - `baseline.py` — MA7 baseline
  - `xgb_three.py` — Three‑head XGBoost trainer/inference + artifact I/O + tuning helper
- `services/forecast/app/db/` — DAL
  - `dal.py` — Optional Postgres access (training_runs, model_versions, forecasts, forecast_items, forecast_accuracy)
- `services/forecast/db/migrations/V1__init.sql` — Forecast DB schema

## Forecast DB Schema

- `model_versions`
  - Registry of trained models. Columns: `model_version_id (PK)`, `model_name`, `version_label`, `algorithm`, `trained_at`, `artifact_path`, `training_window_days`, `hyperparams (JSONB)`, `metrics (JSONB)`, `is_active`.
  - One row per version. Exactly one can be active at a time.
  - New versions auto‑activate on successful training unless disabled.

- `training_runs`
  - Tracks training job executions. Columns: `training_run_id (PK)`, `model_version_id (FK nullable)`, `scope ('global'|'product')`, `product_id`, `started_at`, `finished_at`, `status ('queued'|'running'|'succeeded'|'failed')`, `params (JSONB)`, `metrics (JSONB)`, `error_message`.
  - `model_version_id` filled after a successful run.
  - Timestamp semantics: `started_at` is set when transitioning to RUNNING; `finished_at` when SUCCEEDED/FAILED.

- `forecasts`
  - Header for a forecast request. Columns: `forecast_id (PK)`, `requested_at`, `as_of_date`, `horizon_days`, `model_version_id (FK)`, `request_hash (unique)`, `created_by`, `note`.
  - One row per forecast call.

- `forecast_items`
  - Per‑product, per‑day results. Columns: `forecast_item_id (PK)`, `forecast_id (FK -> forecasts ON DELETE CASCADE)`, `product_id`, `forecast_date`, `yhat`, `confidence (JSONB NOT NULL)`, `lower_bound`, `upper_bound`.
  - Index on `(product_id, forecast_date)`.
  - For countable UoMs, `yhat` is an integer (rounded in service before insert).

- `forecast_accuracy`
  - Backtesting and realized accuracy logging. Columns: `accuracy_id (PK)`, `product_id`, `forecast_date`, `horizon_days`, `predicted_value`, `actual_value`, `error_pct`, `model_version_id (FK)`, `calculated_at`.

Relationships:
- `training_runs.model_version_id` → `model_versions.model_version_id` (nullable until a run succeeds)
- `forecasts.model_version_id` → `model_versions.model_version_id`
- `forecast_items.forecast_id` → `forecasts.forecast_id` (cascade delete)
- `forecast_accuracy.model_version_id` → `model_versions.model_version_id`

## APIs

Forecast Service (FastAPI):

- Health
  - `GET /health` → `{ "status": "ok" }`

- Training
  - `POST /train` — Queues background training. Body: `{ "algorithm": "xgb_three" | "baseline", "trainWindowDays": 365|1095, ... }` → `{ taskId, status, statusUrl }`
    - xgb_three honors `hyperparams` and `autoActivate` (default true).
  - `POST /train/tune` — One‑off random search; returns best hyperparams/metrics and can train+activate the best in the same call.
  - `GET /train/status/{taskId}` — PoC stub: returns `{ status: 'queued' }` unless wired to a task store
  - `GET /train/models/active` — Returns the currently active model version (if any)
  - `POST /train/activate/{modelVersionId}` — Marks a model version active; deactivates others
  - `GET /train/models` — Lists model versions (desc by `trained_at`)
  - `GET /train/models/{modelVersionId}` — Returns a single model version

- Forecast
  - `POST /forecast` — Computes forecasts for given products and horizon.
    - Request: `{ "productIds": [..], "horizonDays": 1|7|14, "asOfDate"?: YYYY‑MM‑DD, "returnDaily"?: true }`
    - Response: `{ forecastId, forecasts: [ { productId, daily[], sum, predictionInterval, confidence } ], modelVersion, modelType, generatedAt }`
    - Behavior: uses active `xgb_three` if present; otherwise falls back to MA7 baseline.
    - Rounding: for countable UoMs (adet, koli, paket, çuval, şişe) daily and sum are rounded to integers; PI bounds rounded ≥ 0.
    - Tip: if Inventory has no recent data (e.g., simulator ended), set `asOfDate` near the last actual date so lags/MA are informative.

  - Forecast history (retrieve previous runs for a product)
    - `GET /forecast/history` — list summaries
      - Query: `productId` (required), `asOfFrom?`, `asOfTo?`, `horizonDays?`, `limit?=10`, `offset?=0`
      - Returns: `[ { forecastId, asOfDate, horizonDays, requestedAt, modelVersion, sumYhat } ]`
    - `GET /forecast/history/{forecastId}/items?productId=...` — run details with daily items
      - Returns: `{ forecastId, productId, asOfDate, horizonDays, requestedAt, modelVersion, items: [{date, yhat, lower, upper, confidence}] }`
    - `GET /forecast/history/latest?productId=...&asOfDate=...&horizonDays=...` — latest run by asOf/horizon
      - Returns: the same run detail or null if none

External (Inventory Service) — consumed by Forecast:
- `GET /api/v1/reporting/product-day-sales?from&to&productId` — daily sales by product
- `GET /api/v1/reporting/product-day-promo?from&to&productId` — daily promo percent by product
- `GET /api/v1/reporting/day-offer-stats?from&to` — day‑level offer pressure (no product filter)

## Tips & Troubleshooting

- Prefer single API worker (`--workers 1`) while training to avoid duplicate background tasks.
- If psql tries a Unix socket, use `-h localhost` to force TCP to your container.
- If training fetches no data: shorten `trainWindowDays` to match your dataset and ensure Inventory views have data up to today.
- Forecast client uses `productId` in query params to match Inventory; keep this aligned with the Inventory controller.
