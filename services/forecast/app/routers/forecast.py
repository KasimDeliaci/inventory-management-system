from __future__ import annotations

from datetime import date, timedelta
from typing import List
from fastapi import APIRouter, HTTPException

from ..schemas import ForecastRequest, ForecastResponse, ForecastPerProduct, DailyForecast
from ..clients.inventory import InventoryClient
from ..features.calendar import build_calendar
from ..features.assembler import join_sales_promo_calendar, add_lags_ma
from ..models.baseline import ma7_forecast


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
        yhat = ma7_forecast(hist_df.sort_values("date"), req.horizonDays)

        # Build daily dates
        daily = []
        for i in range(1, req.horizonDays + 1):
            d = as_of + timedelta(days=i)
            daily.append(DailyForecast(date=d, yhat=float(yhat[i - 1])))

        per_product.append(
            ForecastPerProduct(productId=pid, daily=daily, sum=float(sum(yhat)))
        )

    return ForecastResponse(forecasts=per_product, modelVersion="baseline-0.1")

