# Forecast Service

FastAPI service for daily demand forecasting. Trains models and serves synchronous forecasts via REST. Reads Inventory reporting endpoints over HTTP only (no direct DB links).

## Highlights
- REST-only to Inventory: `/api/v1/reporting/product-day-sales`, `/api/v1/reporting/product-day-promo`, `/api/v1/reporting/day-offer-stats`
- Features: calendar flags (in-service), promoPct, offerActiveShare, day-offer stats, lags/MA
- Models: baseline MA7 fallback + XGBoost three-head model (`xgb_three`: h1, y7-sum, y14-sum)
- Storage: filesystem for model artifacts under `MODEL_DIR` + Postgres for registry and forecasts
- Background training: in-process; auto-activates the newly trained model by default
- Countable UoMs are rounded (adet, koli, paket, çuval, şişe) for daily and sums

## Architecture & Data Flow
- Inventory provides operational data and reporting views (sales by day/product, promos, day-level offer pressure).
- Forecast pulls features from Inventory over HTTP, builds per-product training matrices, and trains global models.
- Trained artifacts are saved on disk; model registry and forecast results are persisted in `forecast_db`.
- Forecast endpoint loads the single active model and returns point forecasts, prediction intervals and confidence.

## Run (dev)

1) Install deps:
   `pip install -r services/forecast/requirements.txt`

2) Start API (include :app):
   `uvicorn services.forecast.app.main:app --reload --port 8100`

3) Training runs as in-process background tasks (no extra worker). For stability while training, prefer a single API worker:
   `uvicorn services.forecast.app.main:app --port 8100 --workers 1`

## Env Vars
- `INVENTORY_BASE_URL` (e.g., http://localhost:8000)
- `MODEL_DIR` (default: ./services/forecast/models)
- `FORECAST_DB_DSN` (Postgres, e.g., `postgresql://user:pass@localhost:5432/forecast_db`)

Tips
- Keep DSN as a single line (no newlines). Test it with: `psql "$FORECAST_DB_DSN" -c '\dt'`.
- Start Uvicorn with module:attribute import string, e.g. `services.forecast.app.main:app`.

## API
- POST `/train` (async): queue training; body `{algorithm:'xgb_three'|'baseline', trainWindowDays?:int, hyperparams?:{}, autoActivate?:bool}` → `{taskId}`
  - xgb_three honors `hyperparams` (e.g., max_depth, eta/learning_rate, subsample, colsample_bytree, min_child_weight, reg_lambda, reg_alpha, n_estimators, early_stopping_rounds)
  - On success the new model auto-activates (set `autoActivate:false` to skip)
- POST `/train/tune` (sync): one-off random search; query/body params `trials`, `trainWindowDays`, `seed`, `asOfDate?`, `trainBest` (default true). Returns best hyperparams/metrics and optionally trains+activates the best.
- GET  `/train/models/active` — current active model metadata
- POST `/train/activate/{modelVersionId}` — activate a specific version (deactivates others)
- GET  `/train/models` — list model versions (desc by trained_at)
- GET  `/train/models/{modelVersionId}` — get model version details
- POST `/forecast` (sync): `{productIds, horizonDays(1|7|14), asOfDate?, returnDaily?}` → per-product {daily[], sum, predictionInterval, confidence}
  - Uses active xgb_three if present; fallback MA7 otherwise
  - For countable UoMs, daily and sum are rounded to integers; PI bounds are rounded >= 0
  - Tip: if Inventory has no recent data, pass `asOfDate` near last actuals

### Curl examples
- Train (36 months, custom params):
```
curl -X POST 'http://127.0.0.1:8100/train' \
  -H 'Content-Type: application/json' \
  -d '{"algorithm":"xgb_three","trainWindowDays":1095,"hyperparams":{"max_depth":6,"learning_rate":0.05,"subsample":0.8,"colsample_bytree":0.8,"n_estimators":1000}}'
```
- Tune (20 trials, auto-train best):
```
curl -X POST 'http://127.0.0.1:8100/train/tune?trials=20&trainWindowDays=1095&seed=42&trainBest=true'
```
- Forecast (anchor asOf to last known actuals if needed):
```
curl -X POST 'http://127.0.0.1:8100/forecast' \
  -H 'Content-Type: application/json' \
  -d '{"productIds":[1001,1008],"horizonDays":7,"asOfDate":"2025-06-30","returnDaily":true}'
```

## Notes
- Calendar flags are computed locally (same logic as `calendar_events.py`).
- Confidence: volatility-based score. PI: conformal residuals per horizon head.
- Router aligns history window with model inference when xgb_three is active.
- Root `/` redirects to `/docs`; `/favicon.ico` returns 204.

### Features & Targets (xgb_three)
- h1 daily model: uses lags/MA and exogenous for t+1.
- y7/y14 sum models: use window aggregates of exogenous + calendar counts.
- Training window is configurable; inference uses `max(365, training_window_days/3)` for history alignment.

### Rounding (countable UoMs)
- For UoMs in {adet, koli, paket, çuval, şişe}, daily yhat is rounded to nearest integer ≥ 0, sum recomputed from rounded dailies. PI bounds are also integer and ≥ 0 (sum-level only).

### Confidence details
- Volatility (IQR-trimmed coefficient of variation), Trend (Spearman + monotonicity, last 30d), Weekly seasonality proxy, Data completeness.
- Blend weights: 30% volatility, 25% trend, 10% weekly, 35% completeness.

## Future Plan
- Weekly retraining via cron/K8s Job.
- Backtesting and accuracy tracking: compute daily errors into `forecast_accuracy`.

## Troubleshooting
- 404 on `/`: visit `/docs` or use `/health`.
- Import error: ensure `:app` is present in the Uvicorn command.
- JSON 422 on `/train`: send valid JSON like `{ "algorithm":"xgb_three","trainWindowDays":1095 }`.
- DB 500s: verify `FORECAST_DB_DSN` is exported in the same shell as Uvicorn and the migration ran.
- Very small forecasts near “today”: if Inventory data ends earlier (e.g., 2025‑06‑30), pass `asOfDate` near the last actuals.
