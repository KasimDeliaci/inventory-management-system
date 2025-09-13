from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta
from typing import Dict, Iterable, List, Optional, Tuple

import pandas as pd


def _safe_dt(d: object) -> date:
    return pd.to_datetime(d).date()


def build_date_indexed(df: pd.DataFrame, date_col: str = "date") -> pd.DataFrame:
    out = df.copy()
    if date_col in out.columns:
        out[date_col] = pd.to_datetime(out[date_col]).dt.normalize()
        out = out.set_index(date_col).sort_index()
    return out


def densify_spine(start: date, end: date) -> pd.DataFrame:
    idx = pd.date_range(start, end, freq="D")
    return pd.DataFrame(index=idx)


def aggregate_window(
    start_excl: date,
    horizon_days: int,
    promo_by_day: pd.DataFrame,
    offer_by_day: pd.DataFrame,
    calendar_by_day: pd.DataFrame,
) -> Dict[str, float]:
    """Aggregate exogenous signals over the future window (t+1..t+H).

    Inputs are date-indexed frames with columns:
      - promo_by_day: promoPct (0..100)
      - offer_by_day: offerAvgPct (0..100), offerMaxPct (0..100), activeOffersCount (int)
      - calendar_by_day: is_weekend (bool), is_official_holiday (bool), is_ramadan, is_eid_fitr, is_eid_adha,
                         is_valentines, is_mothers_day, is_teachers_day, is_ataturk_memorial, is_black_friday, is_back_to_school
    """
    start = pd.Timestamp(start_excl) + pd.Timedelta(days=1)
    end = start + pd.Timedelta(days=horizon_days - 1)

    # Slice
    p = promo_by_day.loc[start:end] if len(promo_by_day) else pd.DataFrame()
    o = offer_by_day.loc[start:end] if len(offer_by_day) else pd.DataFrame()
    c = calendar_by_day.loc[start:end] if len(calendar_by_day) else pd.DataFrame()

    res: Dict[str, float] = {}

    # Promo window
    if not p.empty and "promoPct" in p.columns:
        s = pd.to_numeric(p["promoPct"], errors="coerce").fillna(0.0)
        res["promo_pct_avg"] = float(s.mean())
        res["promo_pct_max"] = float(s.max())
        res["promo_days_count"] = int((s > 0).sum())
    else:
        res.update({"promo_pct_avg": 0.0, "promo_pct_max": 0.0, "promo_days_count": 0})

    # Offer window
    if not o.empty:
        for col in ("offerAvgPct", "offerMaxPct"):
            if col in o.columns:
                s = pd.to_numeric(o[col], errors="coerce").fillna(0.0)
                res[f"{col}_avg"] = float(s.mean())
                res[f"{col}_max"] = float(s.max())
            else:
                res[f"{col}_avg"] = 0.0
                res[f"{col}_max"] = 0.0
        if "activeOffersCount" in o.columns:
            s = pd.to_numeric(o["activeOffersCount"], errors="coerce").fillna(0.0)
            res["active_offers_count_avg"] = float(s.mean())
            res["active_offers_count_max"] = float(s.max())
        else:
            res["active_offers_count_avg"] = 0.0
            res["active_offers_count_max"] = 0.0
    else:
        res.update({
            "offerAvgPct_avg": 0.0,
            "offerAvgPct_max": 0.0,
            "offerMaxPct_avg": 0.0,
            "offerMaxPct_max": 0.0,
            "active_offers_count_avg": 0.0,
            "active_offers_count_max": 0.0,
        })

    # Calendar window counts
    flags = [
        "is_weekend",
        "is_official_holiday",
        "is_ramadan",
        "is_eid_fitr",
        "is_eid_adha",
        "is_valentines",
        "is_mothers_day",
        "is_teachers_day",
        "is_ataturk_memorial",
        "is_black_friday",
        "is_back_to_school",
    ]
    if not c.empty:
        for f in flags:
            if f in c.columns:
                s = c[f].astype(bool)
                res[f"{f}_count"] = int(s.sum())
            else:
                res[f"{f}_count"] = 0
    else:
        for f in flags:
            res[f"{f}_count"] = 0

    return res


def daily_weights(
    start_excl: date,
    horizon_days: int,
    promo_by_day: pd.DataFrame,
    offer_by_day: pd.DataFrame,
    calendar_by_day: pd.DataFrame,
    alpha_promo: float = 0.3,
    alpha_offer: float = 0.2,
    alpha_weekend: float = 0.15,
    alpha_holiday: float = 0.25,
) -> List[float]:
    """Compute simple positive weights for each future day t+1..t+H.
    Weight ∝ 1 + αp*promoPct + αo*offerAvgPct + αw*is_weekend + αh*is_official_holiday.
    promo/offer are scaled 0..1 (percent/100).
    """
    start = pd.Timestamp(start_excl) + pd.Timedelta(days=1)
    end = start + pd.Timedelta(days=horizon_days - 1)

    p = promo_by_day.loc[start:end] if len(promo_by_day) else pd.DataFrame()
    o = offer_by_day.loc[start:end] if len(offer_by_day) else pd.DataFrame()
    c = calendar_by_day.loc[start:end] if len(calendar_by_day) else pd.DataFrame()

    out: List[float] = []
    days = pd.date_range(start, end, freq="D")
    for d in days:
        promo = float(p.loc[d, "promoPct"]) if (not p.empty and d in p.index and "promoPct" in p.columns) else 0.0
        offer = float(o.loc[d, "offerAvgPct"]) if (not o.empty and d in o.index and "offerAvgPct" in o.columns) else 0.0
        is_wknd = bool(c.loc[d, "is_weekend"]) if (not c.empty and d in c.index and "is_weekend" in c.columns) else False
        is_hol = bool(c.loc[d, "is_official_holiday"]) if (not c.empty and d in c.index and "is_official_holiday" in c.columns) else False
        w = 1.0 + alpha_promo * (promo / 100.0) + alpha_offer * (offer / 100.0)
        if is_wknd:
            w += alpha_weekend
        if is_hol:
            w += alpha_holiday
        out.append(max(0.0, float(w)))

    s = sum(out) or 1.0
    return [w / s for w in out]

