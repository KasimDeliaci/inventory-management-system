from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Dict, Iterable, List, Optional

import pandas as pd
import holidays


def build_calendar(start: date, end: date, country: str = "TR") -> pd.DataFrame:
    years = list(range(start.year, end.year + 1))
    tr_holidays = holidays.Turkey(years=years) if country == "TR" else holidays.country_holidays(country, years=years)

    rows = []
    d = start
    while d <= end:
        dow = d.weekday()
        is_weekend = dow >= 5
        hname = tr_holidays.get(d)
        is_official = hname is not None

        # Simple season label
        if d.month in (12, 1, 2):
            season = "winter"
        elif d.month in (3, 4, 5):
            season = "spring"
        elif d.month in (6, 7, 8):
            season = "summer"
        else:
            season = "autumn"

        # Eid & Ramadan (approx) using holiday labels
        hn = str(hname) if hname else ""
        is_eid_fitr = "Eid al-Fitr" in hn
        is_eid_adha = "Eid al-Adha" in hn
        # Approx Ramadan as 30 days ending before first Eid al-Fitr day in that year
        # We recompute on the fly per day by scanning holidays for the year
        is_ramadan = False
        try:
            fitr_first = min([h for h, n in tr_holidays.items() if h.year == d.year and "Eid al-Fitr" in str(n)])
            if fitr_first:
                is_ramadan = (fitr_first - timedelta(days=30) <= d < fitr_first)
        except ValueError:
            pass

        rows.append({
            "date": d,
            "year": d.year,
            "month": d.month,
            "day": d.day,
            "dow": dow,
            "is_weekend": is_weekend,
            "season": season,
            "is_official_holiday": is_official,
            "holiday_names": hn,
            "is_ramadan": is_ramadan,
            "is_eid_fitr": is_eid_fitr,
            "is_eid_adha": is_eid_adha,
            "is_valentines": (d.month == 2 and d.day == 14),
            "is_mothers_day": _second_sunday_of_may(d.year) == d,
            "is_teachers_day": (d.month == 11 and d.day == 24),
            "is_ataturk_memorial": (d.month == 11 and d.day == 10),
            "is_black_friday": _last_friday_of_november(d.year) == d,
            "is_back_to_school": (date(d.year, 8, 25) <= d <= date(d.year, 9, 15)),
        })
        d += timedelta(days=1)

    df = pd.DataFrame(rows)
    df.sort_values("date", inplace=True)
    return df


def _second_sunday_of_may(year: int) -> date:
    d = date(year, 5, 1)
    first_sunday = d + timedelta(days=(6 - d.weekday()) % 7)
    return first_sunday + timedelta(days=7)


def _last_friday_of_november(year: int) -> date:
    d = date(year, 11, 30)
    while d.weekday() != 4:
        d -= timedelta(days=1)
    return d

