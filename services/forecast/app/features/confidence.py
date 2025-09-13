from __future__ import annotations

from typing import Dict, Any

import numpy as np
import pandas as pd


def calculate_confidence(sales: pd.Series) -> Dict[str, Any]:
    """Compute a volatility-based confidence score with simple guards.

    Expects a pandas Series of daily sales where the index is a daily DateIndex
    (or an integer-like monotonic index). Handles short series and outliers.
    Returns a dict with keys: score, level, factors, recommendation.
    """
    if sales is None:
        return {
            "score": 20,
            "level": "low",
            "factors": {"insufficient_data": True, "days_available": 0},
            "recommendation": "needs_review",
        }

    # Ensure numeric dtype
    s = pd.to_numeric(sales, errors="coerce").dropna()
    n = int(len(s))
    if n < 7:
        return {
            "score": 20,
            "level": "low",
            "factors": {"insufficient_data": True, "days_available": n},
            "recommendation": "needs_review",
        }

    # 1) Outlier trimming via IQR
    q1, q3 = s.quantile([0.25, 0.75])
    iqr = float(q3 - q1) or 1.0
    trimmed = s[(s >= q1 - 1.5 * iqr) & (s <= q3 + 1.5 * iqr)]
    mean_sales = float(trimmed.mean() or 0.0)
    std_sales = float(trimmed.std(ddof=1) or 0.0)

    # 2) Normalized CV with zero-mean safety and smoother scaling
    cv = std_sales / (mean_sales + 1.0)
    cv_scaled = cv / (cv + 0.5)  # in [0,1)
    volatility_score = max(0.0, 100.0 * (1.0 - min(1.0, cv_scaled)))

    # 3) Recent trend stability (last 30d), step-aware
    recent = s.tail(30)
    if len(recent) >= 14:
        x = pd.Series(range(len(recent)), dtype=float)
        y = recent.reset_index(drop=True).astype(float)
        # Spearman (rank) correlation is more robust to step changes
        try:
            corr_s = float(x.corr(y, method="spearman")) if y.nunique() > 1 else 0.0
        except Exception:
            corr_s = 0.0
        # Monotonicity ratio on a lightly smoothed series
        y_smooth = y.ewm(alpha=0.3, adjust=False).mean()
        diffs = np.sign(np.diff(y_smooth.to_numpy(dtype=float)))
        monot = float(abs(diffs.sum()) / len(diffs)) if len(diffs) > 0 else 0.0  # in [0,1]
        trend_stability = min(100.0, 50.0 + 25.0 * abs(corr_s) + 25.0 * monot)
    else:
        trend_stability = 50.0

    # 4) Seasonality strength (weekly pattern proxy)
    if isinstance(s.index, pd.DatetimeIndex) and len(s) >= 90:
        dow_std = s.groupby(s.index.dayofweek).std().mean()
        base_std = float(s.std(ddof=1) or 1.0)
        seasonality_score = max(0.0, 100.0 * (1.0 - float(dow_std or 0.0) / base_std))
    else:
        seasonality_score = 50.0

    # 5) Data completeness over observed time window
    if isinstance(s.index, pd.DatetimeIndex):
        expected_days = int((s.index.max() - s.index.min()).days) + 1
        completeness = min(100.0, 100.0 * len(s) / max(1, expected_days))
    else:
        completeness = min(100.0, 100.0 * len(s) / 365.0)

    # Weighted overall (weekly seasonality de-weighted, completeness slightly upweighted)
    # Weights: volatility 30%, trend 20%, weekly-seasonality 10%, data completeness 40%
    overall = (
        0.30 * volatility_score
        + 0.20 * trend_stability
        + 0.10 * seasonality_score
        + 0.4 * completeness
    )
    score = int(round(overall))
    level = "high" if score >= 70 else ("medium" if score >= 50 else "low")
    recommendation = (
        "reliable_for_planning" if level == "high" else ("use_with_caution" if level == "medium" else "needs_review")
    )

    return {
        "score": score,
        "level": level,
        "factors": {
            "volatility": int(round(volatility_score)),
            "trend": int(round(trend_stability)),
            "seasonality": int(round(seasonality_score)),
            "data_quality": int(round(completeness)),
            "days_available": n,
        },
        "recommendation": recommendation,
    }
