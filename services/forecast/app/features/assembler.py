from __future__ import annotations

from datetime import date, timedelta
from typing import Iterable, List, Dict

import pandas as pd


def densify_date_spine(start: date, end: date) -> pd.DataFrame:
    dates = pd.date_range(start, end, freq="D").date
    return pd.DataFrame({"date": dates})


def join_sales_promo_calendar(
    sales_rows: List[dict],
    promo_rows: List[dict],
    calendar_df: pd.DataFrame,
    product_id: int,
) -> pd.DataFrame:
    sales = pd.DataFrame(sales_rows)
    promos = pd.DataFrame(promo_rows)
    sales = sales[sales["productId"] == product_id] if not sales.empty else pd.DataFrame(columns=["date","productId","salesUnits","offerActiveShare"]) 
    promos = promos[promos["productId"] == product_id] if not promos.empty else pd.DataFrame(columns=["date","productId","promoPct"]) 

    # Normalize types
    if not sales.empty:
        sales["date"] = pd.to_datetime(sales["date"]).dt.date
    if not promos.empty:
        promos["date"] = pd.to_datetime(promos["date"]).dt.date
    cal = calendar_df.copy()
    cal["date"] = pd.to_datetime(cal["date"]).dt.date

    # Densify spine over union of calendars
    start = min([cal["date"].min()] + ([sales["date"].min()] if not sales.empty else []) + ([promos["date"].min()] if not promos.empty else []))
    end = max([cal["date"].max()] + ([sales["date"].max()] if not sales.empty else []) + ([promos["date"].max()] if not promos.empty else []))
    spine = densify_date_spine(start, end)

    df = spine.merge(sales[["date","salesUnits","offerActiveShare"]], on="date", how="left")
    df = df.merge(promos[["date","promoPct"]], on="date", how="left")
    df = df.merge(cal, on="date", how="left")
    df["salesUnits"] = df["salesUnits"].fillna(0.0)
    df["offerActiveShare"] = df["offerActiveShare"].fillna(0.0)
    df["promoPct"] = df["promoPct"].fillna(0.0)
    return df


def add_lags_ma(df: pd.DataFrame, cols: List[str] = ["salesUnits"]) -> pd.DataFrame:
    sdf = df.sort_values("date").copy()
    for col in cols:
        sdf[f"{col}_lag7"] = sdf[col].shift(7)
        sdf[f"{col}_lag14"] = sdf[col].shift(14)
        sdf[f"{col}_lag28"] = sdf[col].shift(28)
        sdf[f"{col}_ma7"] = sdf[col].rolling(window=7, min_periods=1).mean().shift(1)
        sdf[f"{col}_ma14"] = sdf[col].rolling(window=14, min_periods=1).mean().shift(1)
        sdf[f"{col}_ma28"] = sdf[col].rolling(window=28, min_periods=1).mean().shift(1)
    return sdf

