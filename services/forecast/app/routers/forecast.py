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
)
from ..clients.inventory import InventoryClient
from ..features.calendar import build_calendar
from ..features.assembler import join_sales_promo_calendar, add_lags_ma
from ..models.baseline import ma7_forecast
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
    start_hist = as_of - timedelta(days=365)
    end_hist = as_of
    horizon_end = as_of + timedelta(days=req.horizonDays)

    inv = InventoryClient()
    # Pull history for all requested products (single call for efficiency)
    sales_rows = inv.get_product_day_sales(start_hist, end_hist, req.productIds)
    promo_rows_hist = inv.get_product_day_promo(start_hist, end_hist, req.productIds)
    promo_rows_future = inv.get_product_day_promo(as_of + timedelta(days=1), horizon_end, req.productIds)

    # Calendar for history+horizon
    cal = build_calendar(start_hist, horizon_end)

    per_product: List[ForecastPerProduct] = []
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
            # MA7 one-step prediction over tail (shifted rolling mean)
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

        # Build daily dates
        daily = []
        for i in range(1, req.horizonDays + 1):
            d = as_of + timedelta(days=i)
            daily.append(DailyForecast(date=d, yhat=float(yhat[i - 1])))

        # Aggregate prediction interval across horizon (naive sum of residual bounds)
        sum_y = float(sum(yhat))
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
    mv = dal.get_active_model_version()
    try:
        fid = dal.insert_forecast(as_of.isoformat(), req.horizonDays, (mv or {}).get("model_version_id"), None)
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
