from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Dict, Iterable, List, Optional, Tuple

import pandas as pd
import holidays


@dataclass
class WorldMeta:
    start_date: date
    end_date: date
    country: str = "TR"


def _parse_meta(meta: Dict) -> WorldMeta:
    sd = datetime.fromisoformat(str(meta.get("start_date"))).date()
    ed = datetime.fromisoformat(str(meta.get("end_date"))).date()
    return WorldMeta(start_date=sd, end_date=ed, country=str(meta.get("country", "TR")))


def _date_range_inclusive(start: date, end: date) -> Iterable[date]:
    d = start
    delta = timedelta(days=1)
    while d <= end:
        yield d
        d += delta


def _years_in_range(start: date, end: date) -> List[int]:
    return list(range(start.year, end.year + 1))


def _turkey_holidays(years: List[int]) -> holidays.HolidayBase:
    # Turkey public holidays (returns mapping of date->name)
    return holidays.Turkey(years=years)


def _find_first_day_by_keywords(hmap: Dict[date, str], keywords: Iterable[str]) -> Dict[int, Optional[date]]:
    """Find the first holiday date per year whose name contains any of the keywords.

    Keywords are matched case-insensitively and tolerate language variants, e.g.,
      ['Ramazan Bayram', 'Şeker Bayram', 'Eid al-Fitr']
    """
    kw = [k.lower() for k in keywords]
    by_year: Dict[int, List[Tuple[date, str]]] = {}
    for d, name in hmap.items():
        n = str(name).lower()
        if any(k in n for k in kw):
            by_year.setdefault(d.year, []).append((d, name))

    result: Dict[int, Optional[date]] = {}
    for y, items in by_year.items():
        # Prefer explicit first-day markers in Turkish '(1. Gün)' or English '(1st day)'
        firsts = [d for (d, n) in items if ("1. Gün" in n) or ("1st" in n and "day" in n.lower())]
        if firsts:
            result[y] = min(firsts)
            continue

        # Next prefer non-eve entries (exclude 'Arifesi'/'Eve')
        non_eve = [d for (d, n) in items if ("arife" not in n.lower()) and ("eve" not in n.lower())]
        if non_eve:
            result[y] = min(non_eve)
            continue

        # Fallback: earliest matching date in year
        result[y] = min(d for (d, _) in items)
    return result


def _ramadan_window_for_year(eid_first_day: Optional[date]) -> Optional[Tuple[date, date]]:
    """Approximate Ramadan as the 30-day period ending the day before Eid al-Fitr."""
    if eid_first_day is None:
        return None
    end = eid_first_day - timedelta(days=1)
    start = eid_first_day - timedelta(days=30)
    return (start, end)


def _second_sunday_of_may(year: int) -> date:
    d = date(year, 5, 1)
    # 0=Mon..6=Sun, find first Sunday
    first_sunday = d + timedelta(days=(6 - d.weekday()) % 7)
    return first_sunday + timedelta(days=7)


def _last_friday_of_november(year: int) -> date:
    d = date(year, 11, 30)
    # go backwards to Friday (4)
    while d.weekday() != 4:
        d -= timedelta(days=1)
    return d


def _season_label(d: date) -> str:
    m = d.month
    if m in (12, 1, 2):
        return "winter"
    if m in (3, 4, 5):
        return "spring"
    if m in (6, 7, 8):
        return "summer"
    return "autumn"  # 9,10,11


def build_calendar(meta_dict: Dict) -> pd.DataFrame:
    """Build daily calendar with official holidays, weekends, seasonal labels, and special commercial days.

    Output columns:
      date, year, month, day, dow, is_weekend, season,
      is_official_holiday, holiday_names,
      is_ramadan, is_eid_fitr, is_eid_adha,
      is_valentines, is_mothers_day, is_teachers_day, is_ataturk_memorial,
      is_black_friday, is_back_to_school
    """
    meta = _parse_meta(meta_dict)
    years = _years_in_range(meta.start_date, meta.end_date)
    tr_holidays = _turkey_holidays(years)

    # Precompute Eid first days and Ramadan windows per year
    eid_fitr_by_year = _find_first_day_by_keywords(
        tr_holidays,
        ["Ramazan Bayram", "Şeker Bayram", "Eid al-Fitr"],
    )
    eid_adha_by_year = _find_first_day_by_keywords(
        tr_holidays,
        ["Kurban Bayram", "Eid al-Adha"],
    )
    ramadan_windows: Dict[int, Tuple[date, date]] = {}
    for y in years:
        rw = _ramadan_window_for_year(eid_fitr_by_year.get(y))
        if rw:
            ramadan_windows[y] = rw

    rows = []
    for d in _date_range_inclusive(meta.start_date, meta.end_date):
        dow = d.weekday()  # 0=Mon..6=Sun
        is_weekend = dow >= 5

        # Official holiday lookup
        holiday_name = tr_holidays.get(d)
        is_official_holiday = holiday_name is not None
        if holiday_name is None:
            holiday_names = ""
        else:
            holiday_names = str(holiday_name)

        # Eid flags
        is_eid_fitr = False
        is_eid_adha = False
        if d.year in eid_fitr_by_year and eid_fitr_by_year[d.year] is not None:
            fitr_first = eid_fitr_by_year[d.year]
            # Eid al-Fitr typically 3 days
            if fitr_first and 0 <= (d - fitr_first).days <= 2:
                is_eid_fitr = True
        if d.year in eid_adha_by_year and eid_adha_by_year[d.year] is not None:
            adha_first = eid_adha_by_year[d.year]
            # Eid al-Adha typically 4 days
            if adha_first and 0 <= (d - adha_first).days <= 3:
                is_eid_adha = True

        # Ramadan window flag (approximate)
        is_ramadan = False
        rw = ramadan_windows.get(d.year)
        if rw:
            is_ramadan = (rw[0] <= d <= rw[1])

        # Special commercial/commemorative days
        is_valentines = (d.month == 2 and d.day == 14)
        is_mothers_day = (d == _second_sunday_of_may(d.year))
        is_teachers_day = (d.month == 11 and d.day == 24)
        is_ataturk_memorial = (d.month == 11 and d.day == 10)
        is_black_friday = (d == _last_friday_of_november(d.year))
        # Back-to-school window: Aug 30–Sep 15 (inclusive)
        is_back_to_school = (date(d.year, 8, 30) <= d <= date(d.year, 9, 15))

        rows.append({
            'date': d,
            'year': d.year,
            'month': d.month,
            'day': d.day,
            'dow': dow,
            'is_weekend': is_weekend,
            'season': _season_label(d),
            'is_official_holiday': is_official_holiday,
            'holiday_names': holiday_names,
            'is_ramadan': is_ramadan,
            'is_eid_fitr': is_eid_fitr,
            'is_eid_adha': is_eid_adha,
            'is_valentines': is_valentines,
            'is_mothers_day': is_mothers_day,
            'is_teachers_day': is_teachers_day,
            'is_ataturk_memorial': is_ataturk_memorial,
            'is_black_friday': is_black_friday,
            'is_back_to_school': is_back_to_school,
        })

    df = pd.DataFrame(rows)
    # Ensure stable ordering
    df.sort_values('date', inplace=True)
    return df


def write_calendar(df: pd.DataFrame, outdir) -> str:
    out_calendar_dir = outdir / 'calendar'
    out_calendar_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_calendar_dir / 'daily_calendar.csv'
    df.to_csv(out_path, index=False, date_format='%Y-%m-%d')
    return str(out_path)
