from __future__ import annotations

from datetime import date, timedelta
from typing import List

import pandas as pd


def ma7_forecast(df: pd.DataFrame, horizon: int) -> List[float]:
    """Naive baseline: predict next N days as the last 7-day mean.
    Expects df sorted by date with column 'salesUnits'.
    """
    if df.empty:
        return [0.0] * horizon
    hist = df["salesUnits"].astype(float)
    window = min(7, len(hist))
    mu = float(hist.tail(window).mean()) if window > 0 else 0.0
    return [mu] * horizon

