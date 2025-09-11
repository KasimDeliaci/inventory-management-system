# Forecast Service (PoC)

FastAPI service for daily demand forecasting. Trains models (async) and serves synchronous forecasts via REST. Reads Inventory reporting endpoints over HTTP only (no direct DB links).

## Highlights
- REST-only to Inventory: `/api/v1/reporting/product-day-sales`, `/api/v1/reporting/product-day-promo`
- Daily features: calendar flags (in-service), promoPct, offerActiveShare, lags/MA (builder stub)
- Models: starts with a simple baseline (MA7) for PoC; upgrade to GBM later
- Storage: filesystem for model artifacts (Docker volume friendly)
- Background training: Celery + Redis (minimal)

## Run (dev)

1) Install deps:
   `pip install -r services/forecast/requirements.txt`

2) Start API:
   `uvicorn services.forecast.app.main:app --reload --port 8100`

3) Optional: run Celery worker (training tasks):
   `CELERY_BROKER_URL=redis://localhost:6379/0 \
   celery -A services.forecast.app.workers.celery_app.celery_app worker --loglevel=INFO`

## Env Vars
- `INVENTORY_BASE_URL` (e.g., http://localhost:8000)
- `MODEL_DIR` (default: ./services/forecast/models)
- `FORECAST_DB_DSN` (optional; PoC can skip DB writes)
- `CELERY_BROKER_URL` (default: redis://localhost:6379/0)

## API
- POST `/train` (async): queues training; returns `{taskId}`
- GET  `/train/status/{taskId}`
- POST `/forecast` (sync): `{productIds, horizonDays, asOfDate?}` → daily[] + sum

## Notes
- Calendar flags are computed locally (no CSV), based on the same logic as `calendar_events.py`.
- For PoC, forecasts use a baseline MA7; swap-in real models incrementally.

