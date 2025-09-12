# Forecast Service (PoC)

FastAPI service for daily demand forecasting. Trains models (async) and serves synchronous forecasts via REST. Reads Inventory reporting endpoints over HTTP only (no direct DB links).

## Highlights
- REST-only to Inventory: `/api/v1/reporting/product-day-sales`, `/api/v1/reporting/product-day-promo`
- Daily features: calendar flags (in-service), promoPct, offerActiveShare, lags/MA (builder stub)
- Models: starts with a simple baseline (MA7) for PoC; upgrade to GBM later
- Storage: filesystem for model artifacts (Docker volume friendly)
- Background training: FastAPI BackgroundTasks (simple, in-process)

## Run (dev)

1) Install deps:
   `pip install -r services/forecast/requirements.txt`

2) Start API:
   `uvicorn services.forecast.app.main:app --reload --port 8100`

3) Training runs as in-process background tasks (no extra worker). For stability while training, prefer a single API worker:
   `uvicorn services.forecast.app.main:app --port 8100 --workers 1`

## Env Vars
- `INVENTORY_BASE_URL` (e.g., http://localhost:8000)
- `MODEL_DIR` (default: ./services/forecast/models)
- `FORECAST_DB_DSN` (optional; PoC can skip DB writes)

## API
- POST `/train` (async): queues training using BackgroundTasks; returns `{taskId}`
- GET  `/train/status/{taskId}`
- GET  `/train/models/active` — returns the active model metadata
- POST `/train/activate/{modelVersionId}` — sets a model version active
- GET  `/train/models` — list model versions (desc by trained_at)
- GET  `/train/models/{modelVersionId}` — get model version details
- POST `/forecast` (sync): `{productIds, horizonDays, asOfDate?}` → daily[] + sum

## Notes
- Calendar flags are computed locally (no CSV), based on the same logic as `calendar_events.py`.
- For PoC, forecasts use a baseline MA7; swap-in real models incrementally.
- Confidence and prediction intervals are returned per product using historical volatility and empirical residuals.

## Future Plan
- Weekly retraining: add a simple cron/K8s Job or CLI that hits `/train` and activates the new version after validation.
- Backtesting and accuracy tracking: `forecast_accuracy` table is created to log per-product errors when actuals arrive; add a small job to compute and write errors daily.
