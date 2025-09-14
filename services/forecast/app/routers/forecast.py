from __future__ import annotations

from datetime import date, timedelta
from typing import List, Optional
from fastapi import APIRouter, HTTPException

import pandas as pd

from ..schemas import (
    ForecastRequest,
    ForecastResponse,
    ForecastPerProduct,
    DailyForecast,
    PredictionInterval,
    ForecastRunSummary,
    ForecastHistoryItem,
    ForecastRunDetail,
)
from ..clients.inventory import InventoryClient
from ..features.calendar import build_calendar
from ..features.assembler import join_sales_promo_calendar, add_lags_ma
from ..models.baseline import ma7_forecast
from ..models.xgb_three import load_artifact, infer_xgb_three
from ..features.confidence import calculate_confidence
from ..db import dal


router = APIRouter()


@router.post("", response_model=ForecastResponse)
def forecast(req: ForecastRequest) -> ForecastResponse:
    if req.horizonDays not in (1, 7, 14):
        raise HTTPException(status_code=400, detail="horizonDays must be 1, 7, or 14")
    if not req.productIds:
        raise HTTPException(status_code=400, detail="productIds must be non-empty")

    as_of = req.asOfDate or date.today()
    horizon_end = as_of + timedelta(days=req.horizonDays)

    inv = InventoryClient()

    # Prefer xgb_three if active (determine early to align history window)
    mv = dal.get_active_model_version()
    use_xgb = bool(mv and str(mv.get("algorithm", "")) == "xgb_three" and mv.get("artifact_path"))
    art = load_artifact(str(mv.get("artifact_path"))) if use_xgb else None
    if use_xgb and art is None:
        use_xgb = False

    # History window consistency: align with xgb inference logic when active; fallback to 365d
    hist_days = 365
    if use_xgb and art is not None:
        hist_days = max(365, int(art.training_window_days) // 3)
    start_hist = as_of - timedelta(days=hist_days)
    end_hist = as_of

    # Pull history for all requested products (single call for efficiency)
    sales_rows = inv.get_product_day_sales(start_hist, end_hist, req.productIds)
    promo_rows_hist = inv.get_product_day_promo(start_hist, end_hist, req.productIds)
    promo_rows_future = inv.get_product_day_promo(as_of + timedelta(days=1), horizon_end, req.productIds)

    # Calendar for history+horizon
    cal = build_calendar(start_hist, horizon_end)

    per_product: List[ForecastPerProduct] = []
    # Fetch UoMs to decide rounding for countable products
    COUNTABLE_UOMS = {"adet", "koli", "paket", "çuval", "şişe"}
    uom_map: dict[int, str] = {}
    try:
        uom_map = inv.get_products_uom(req.productIds)
    except Exception:
        uom_map = {}

    def _round_if_countable(pid: int, values: List[float]) -> List[float]:
        u = (uom_map.get(pid) or "").strip().lower()
        if u in COUNTABLE_UOMS:
            return [float(max(0, int(round(v)))) for v in values]
        return values

    # xgb_three path
    if use_xgb:
        if art is not None:
            preds = infer_xgb_three(art, req.productIds, as_of, req.horizonDays)
            # Build per-product confidence using historical series, as before
            for pid in req.productIds:
                # historical frame for confidence
                hist_df = join_sales_promo_calendar(sales_rows, promo_rows_hist, cal, pid)
                sdf = hist_df.sort_values("date")
                try:
                    series = sdf[["date", "salesUnits"]].copy()
                    series["date"] = pd.to_datetime(series["date"]).dt.normalize()
                    s = series.set_index("date")["salesUnits"].astype(float)
                    conf_dict = calculate_confidence(s)
                except Exception:
                    conf_dict = {"score": 20, "level": "low", "factors": {"insufficient_data": True}, "recommendation": "needs_review"}

                pr = preds.get(pid, {"daily": [0.0] * req.horizonDays, "sum": 0.0, "lower": 0.0, "upper": 0.0})
                # Rounding for countable UoMs
                daily_vals = _round_if_countable(pid, [float(x) for x in pr.get("daily", [])])
                sum_val = float(sum(daily_vals)) if daily_vals else float(pr.get("sum", 0.0))
                lower = float(pr.get("lower", 0.0))
                upper = float(pr.get("upper", 0.0))
                # Round PI for countable as well
                u = (uom_map.get(pid) or "").strip().lower()
                if u in COUNTABLE_UOMS:
                    lower = float(max(0, int(round(lower))))
                    upper = float(max(lower, int(round(upper))))
                daily = []
                for i in range(1, req.horizonDays + 1):
                    d = as_of + timedelta(days=i)
                    yv = float(daily_vals[i - 1]) if i - 1 < len(daily_vals) else 0.0
                    daily.append(DailyForecast(date=d, yhat=yv))
                per_product.append(
                    ForecastPerProduct(
                        productId=pid,
                        daily=(daily if req.returnDaily else []),
                        sum=sum_val,
                        predictionInterval=PredictionInterval(lowerBound=lower, upperBound=upper),
                        confidence=conf_dict,  # type: ignore[arg-type]
                    )
                )
        else:
            use_xgb = False

    if not use_xgb:
        for pid in req.productIds:
            # Assemble historical frame
            hist_df = join_sales_promo_calendar(sales_rows, promo_rows_hist, cal, pid)
            hist_df = add_lags_ma(hist_df, ["salesUnits"])  # currently not used by baseline

            # Baseline forecast: MA7
            sdf = hist_df.sort_values("date")
            yhat = ma7_forecast(sdf, req.horizonDays)

            # Confidence based on historical sales series
            try:
                series = sdf[["date", "salesUnits"]].copy()
                series["date"] = pd.to_datetime(series["date"]).dt.normalize()
                s = series.set_index("date")["salesUnits"].astype(float)
                conf_dict = calculate_confidence(s)
            except Exception:
                conf_dict = {"score": 20, "level": "low", "factors": {"insufficient_data": True}, "recommendation": "needs_review"}

            # Simple prediction interval via empirical residual quantiles
            try:
                tail = sdf.tail(90).copy()
                tail["date"] = pd.to_datetime(tail["date"]).dt.normalize()
                tail = tail.set_index("date").sort_index()
                hist_vals = tail["salesUnits"].astype(float)
                ma7 = hist_vals.rolling(window=7, min_periods=1).mean().shift(1)
                residuals = (hist_vals - ma7).dropna()
                if len(residuals) >= 20:
                    q10 = float(residuals.quantile(0.10))
                    q90 = float(residuals.quantile(0.90))
                else:
                    q10 = q90 = 0.0
            except Exception:
                q10 = q90 = 0.0

            # Build daily dates with rounding if countable
            rounded_vals = _round_if_countable(pid, [float(v) for v in yhat])
            daily = []
            for i in range(1, req.horizonDays + 1):
                d = as_of + timedelta(days=i)
                yv = float(rounded_vals[i - 1]) if i - 1 < len(rounded_vals) else 0.0
                daily.append(DailyForecast(date=d, yhat=yv))

            # Aggregate prediction interval across horizon
            sum_y = float(sum(rounded_vals))
            lower = max(0.0, sum_y + req.horizonDays * q10)
            upper = max(lower, sum_y + req.horizonDays * q90)

            per_product.append(
                ForecastPerProduct(
                    productId=pid,
                    daily=(daily if req.returnDaily else []),
                    sum=sum_y,
                    predictionInterval=PredictionInterval(lowerBound=lower, upperBound=upper),
                    confidence=conf_dict,  # type: ignore[arg-type]
                )
            )

    # Optional: persist forecast if DB configured
    fid: Optional[int] = None
    try:
        fid = dal.insert_forecast(as_of.isoformat(), req.horizonDays, ((mv or {}).get("model_version_id") if mv else None), None)
        if fid:
            items = []
            for fp in per_product:
                for df in fp.daily:
                    conf_payload = None
                    try:
                        # pydantic v2
                        conf_payload = fp.confidence.model_dump() if fp.confidence is not None else None  # type: ignore[attr-defined]
                    except Exception:
                        conf_payload = fp.confidence if fp.confidence is not None else None
                    items.append({
                        "productId": fp.productId,
                        "date": df.date.isoformat(),
                        "yhat": df.yhat,
                        "confidence": conf_payload,
                        "lower": (fp.predictionInterval.lowerBound if fp.predictionInterval else None),
                        "upper": (fp.predictionInterval.upperBound if fp.predictionInterval else None),
                    })
            if items:
                dal.insert_forecast_items(fid, items)
    except Exception:
        # Swallow persistence errors in PoC path
        pass

    return ForecastResponse(
        forecasts=per_product,
        modelVersion=(mv["version_label"] if mv else "baseline-0.1"),
        modelType=(mv["algorithm"] if mv else "baseline_ma7"),
    )


@router.get("/history", response_model=list[ForecastRunSummary])
def forecast_history(
    productId: int,
    asOfFrom: Optional[date] = None,
    asOfTo: Optional[date] = None,
    horizonDays: Optional[int] = None,
    limit: int = 10,
    offset: int = 0,
):
    rows = dal.list_forecast_runs_for_product(
        product_id=productId,
        as_of_from=(asOfFrom.isoformat() if asOfFrom else None),
        as_of_to=(asOfTo.isoformat() if asOfTo else None),
        horizon_days=horizonDays,
        limit=limit,
        offset=offset,
    ) or []
    out: list[ForecastRunSummary] = []
    for r in rows:
        out.append(
            ForecastRunSummary(
                forecastId=r["forecast_id"],
                asOfDate=r["as_of_date"],
                horizonDays=r["horizon_days"],
                requestedAt=r["requested_at"],
                modelVersion=r.get("version_label"),
                sumYhat=r.get("sum_yhat", 0.0),
            )
        )
    return out


@router.get("/history/{forecastId}/items", response_model=ForecastRunDetail)
def forecast_history_items(forecastId: int, productId: int):
    # Header info
    header = None
    rows = dal.list_forecast_runs_for_product(productId, None, None, None, limit=1000, offset=0) or []
    for r in rows:
        if int(r["forecast_id"]) == int(forecastId):
            header = r
            break
    if header is None:
        raise HTTPException(status_code=404, detail="Forecast run not found for product")

    items_rows = dal.get_forecast_items_for_product(forecastId, productId) or []
    items: list[ForecastHistoryItem] = []
    for it in items_rows:
        items.append(
            ForecastHistoryItem(
                date=it["date"],
                yhat=it["yhat"],
                lower=it.get("lower"),
                upper=it.get("upper"),
                confidence=it.get("confidence"),
            )
        )
    return ForecastRunDetail(
        forecastId=int(forecastId),
        productId=int(productId),
        asOfDate=header["as_of_date"],
        horizonDays=header["horizon_days"],
        requestedAt=header["requested_at"],
        modelVersion=header.get("version_label"),
        items=items,
    )


@router.get("/history/latest", response_model=Optional[ForecastRunDetail])
def forecast_history_latest(productId: int, asOfDate: date, horizonDays: int):
    head = dal.get_latest_forecast_run(productId, asOfDate.isoformat(), int(horizonDays))
    if not head:
        return None
    items_rows = dal.get_forecast_items_for_product(head["forecast_id"], productId) or []
    items: list[ForecastHistoryItem] = []
    for it in items_rows:
        items.append(
            ForecastHistoryItem(
                date=it["date"],
                yhat=it["yhat"],
                lower=it.get("lower"),
                upper=it.get("upper"),
                confidence=it.get("confidence"),
            )
        )
    return ForecastRunDetail(
        forecastId=head["forecast_id"],
        productId=int(productId),
        asOfDate=head["as_of_date"],
        horizonDays=head["horizon_days"],
        requestedAt=head["requested_at"],
        modelVersion=head.get("version_label"),
        items=items,
    )
