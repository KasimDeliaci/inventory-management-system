from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Dict, Iterable, List, Optional, Tuple

import pandas as pd
from decimal import Decimal, ROUND_HALF_UP


# UoMs that are countable and eligible for BXGY
COUNTABLE_UOMS = {"adet", "koli", "paket", "çuval", "şişe"}


@dataclass
class Campaign:
    campaign_id: int
    campaign_name: str
    campaign_type: str  # 'DISCOUNT' or 'BXGY_SAME_PRODUCT'
    start_date: date
    end_date: date
    discount_percentage: Optional[float] = None
    buy_qty: Optional[int] = None
    get_qty: Optional[int] = None


@dataclass
class CampaignAssignment:
    campaign_id: int
    product_id: int


@dataclass
class CustomerOffer:
    special_offer_id: int
    customer_id: int
    percent_off: float
    start_date: date
    end_date: date


def _rng(seed: Optional[int]) -> random.Random:
    return random.Random(seed if seed is not None else 42)


def _years_in_range(start: date, end: date) -> float:
    return (end - start).days / 365.25


def _product_peak_months(name: str, category: str) -> List[int]:
    n = name.lower()
    c = category.lower()
    # Heuristics for our archetypes
    if "sunscreen" in n:
        return [6, 7, 8]
    if "air conditioner" in n:
        return [6, 7, 8]
    if "ski" in n:
        return [12, 1, 2]
    if "thermo" in n:
        return [11, 12, 1, 2]
    if "dates" in n:
        # Ramadan varies; treat no fixed months here; promos mostly off-season
        return []
    if "chocolate" in n or "chocolates" in n:
        return [2, 5, 11]
    if "parfum" in n or "perfume" in n:
        return [2, 5, 6, 11]
    if "potato chips" in n:
        return [6, 7, 8]
    if "energy drink" in n:
        return []  # weekend-driven more than season
    if "notebook" in n:
        return [8, 9]
    if "flag" in n:
        return [4, 5, 8, 10, 11]
    if "tea" in n:
        return [10, 11, 12, 1, 2]
    # Fallback by category
    if category.lower() in ("appliances",):
        return [6, 7, 8]
    return []


def _non_overlapping_window(rng: random.Random, taken: List[Tuple[date, date]], start: date, end: date, min_days: int, max_days: int,
                            bias_offseason_months: Optional[List[int]] = None, max_tries: int = 500) -> Optional[Tuple[date, date]]:
    total_days = (end - start).days + 1
    if total_days <= 0:
        return None

    def overlaps(a: Tuple[date, date], b: Tuple[date, date]) -> bool:
        return not (a[1] < b[0] or b[1] < a[0])

    tries = 0
    while tries < max_tries:
        tries += 1
        dur = rng.randint(min_days, max_days)
        # Sample a start day with optional month bias (off-season clearance preference)
        if bias_offseason_months:
            # Try biased month first; fallback to uniform if fails many times
            month_choices = bias_offseason_months if rng.random() < 0.7 else list(range(1, 13))
        else:
            month_choices = list(range(1, 13))

        # find candidate start date in allowed range matching chosen month
        # naive approach: sample day index uniformly until month matches
        idx = rng.randint(0, max(0, total_days - 1))
        cand_start = start + timedelta(days=idx)
        # nudge to desired month if not matching
        target_month = rng.choice(month_choices)
        # Adjust year/month by walking forward within range
        if cand_start.month != target_month:
            # move to first day of target_month within same year
            year = cand_start.year
            # Try same year
            try_date = date(year, target_month, 1)
            if try_date < start:
                try_date = date(start.year, target_month, 1)
            if try_date > end:
                try_date = date(end.year, target_month, 1)
            cand_start = try_date

        cand_end = cand_start + timedelta(days=dur - 1)
        if cand_end > end:
            continue
        window = (cand_start, cand_end)
        if all(not overlaps(window, t) for t in taken):
            return window
    return None


def generate_product_campaigns(world: Dict, calendar_df: pd.DataFrame) -> Tuple[List[Campaign], List[CampaignAssignment]]:
    meta = world.get('meta', {})
    products = world.get('products', [])
    seed = meta.get('seed', 42)
    rng = _rng(seed)

    start = pd.to_datetime(meta['start_date']).date()
    end = pd.to_datetime(meta['end_date']).date()
    years = _years_in_range(start, end)

    # Defaults; can be extended by world.yaml policy later
    avg_campaigns_per_year = 3.0
    min_days = 7
    max_days = 21

    campaigns: List[Campaign] = []
    assigns: List[CampaignAssignment] = []
    next_campaign_id = 20001

    for p in products:
        pid = int(p['id'])
        name = str(p['name'])
        category = str(p['category'])
        uom = str(p['uom'])
        peak = _product_peak_months(name, category)

        # Campaign count over the horizon
        n_camp = max(1, int(round(avg_campaigns_per_year * years + rng.uniform(-0.5, 0.5))))
        taken: List[Tuple[date, date]] = []

        # Type mix: BXGY allowed only if countable UoM
        allow_bxgy = uom in COUNTABLE_UOMS
        for _ in range(n_camp):
            # Choose type with bias
            if allow_bxgy:
                ctype = 'DISCOUNT' if rng.random() < 0.6 else 'BXGY_SAME_PRODUCT'
            else:
                ctype = 'DISCOUNT'

            window = _non_overlapping_window(
                rng, taken, start, end, min_days, max_days, bias_offseason_months=[m for m in range(1, 13) if m not in peak] or None
            )
            if not window:
                continue
            w_start, w_end = window

            if ctype == 'DISCOUNT':
                # 10–30% off as whole-number percentages (no min_qty in PoC)
                pct = int(rng.randint(10, 30))  # integer percent for realism
                camp = Campaign(
                    campaign_id=next_campaign_id,
                    campaign_name=f"{name} {int(pct)}% OFF",
                    campaign_type='DISCOUNT',
                    start_date=w_start,
                    end_date=w_end,
                    discount_percentage=pct,
                )
            else:
                # BXGY (2+1) or (3+1) (no min_qty in PoC)
                buy, get = (2, 1) if rng.random() < 0.7 else (3, 1)
                camp = Campaign(
                    campaign_id=next_campaign_id,
                    campaign_name=f"{name} B{buy}G{get}",
                    campaign_type='BXGY_SAME_PRODUCT',
                    start_date=w_start,
                    end_date=w_end,
                    buy_qty=int(buy),
                    get_qty=int(get),
                )

            campaigns.append(camp)
            assigns.append(CampaignAssignment(campaign_id=next_campaign_id, product_id=pid))
            taken.append(window)
            next_campaign_id += 1

    return campaigns, assigns


def _non_overlapping_intervals(rng: random.Random, start: date, end: date, count: int, min_days: int, max_days: int) -> List[Tuple[date, date]]:
    result: List[Tuple[date, date]] = []
    for _ in range(count):
        w = _non_overlapping_window(rng, result, start, end, min_days, max_days)
        if w:
            result.append(w)
    return result


def generate_customer_offers(world: Dict) -> List[CustomerOffer]:
    meta = world.get('meta', {})
    customers = world.get('customers', [])
    seed = meta.get('seed', 42)
    rng = _rng(seed + 7)

    start = pd.to_datetime(meta['start_date']).date()
    end = pd.to_datetime(meta['end_date']).date()
    years = _years_in_range(start, end)

    offers: List[CustomerOffer] = []
    next_offer_id = 30001

    for c in customers:
        cid = int(c['id'])
        # 0–2 offers per year
        n = max(0, int(round(1.0 * years + rng.uniform(-0.5, 0.5))))
        windows = _non_overlapping_intervals(rng, start, end, n, 7, 21)
        for (w_start, w_end) in windows:
            pct = int(round(rng.uniform(5.0, 15.0), 2))
            offers.append(CustomerOffer(
                special_offer_id=next_offer_id,
                customer_id=cid,
                percent_off=pct,
                start_date=w_start,
                end_date=w_end,
            ))
            next_offer_id += 1

    return offers


def emit_campaigns_sql(campaigns: List[Campaign]) -> List[str]:
    lines: List[str] = []
    for c in campaigns:
        # min_qty removed from PoC shape
        cols = ["campaign_id", "campaign_name", "campaign_type", "discount_percentage", "buy_qty", "get_qty", "start_date", "end_date"]
        vals = [
            str(c.campaign_id),
            _s(c.campaign_name),
            _s(c.campaign_type),
            _num_or_null(c.discount_percentage),
            _int_or_null(c.buy_qty),
            _int_or_null(c.get_qty),
            _d(c.start_date),
            _d(c.end_date),
        ]
        lines.append(f"INSERT INTO campaigns({', '.join(cols)}) VALUES ({', '.join(vals)});")
    return lines


def emit_campaign_products_sql(assigns: List[CampaignAssignment]) -> List[str]:
    lines: List[str] = []
    for a in assigns:
        lines.append(
            f"INSERT INTO campaign_products(campaign_id, product_id) VALUES ({a.campaign_id}, {a.product_id});"
        )
    return lines


def emit_customer_offers_sql(offers: List[CustomerOffer]) -> List[str]:
    lines: List[str] = []
    for o in offers:
        cols = ["special_offer_id", "customer_id", "percent_off", "start_date", "end_date"]
        vals = [
            str(o.special_offer_id),
            str(o.customer_id),
            _num_or_null(o.percent_off),
            _d(o.start_date),
            _d(o.end_date),
        ]
        lines.append(f"INSERT INTO customer_special_offers({', '.join(cols)}) VALUES ({', '.join(vals)});")
    return lines


def _s(v: str) -> str:
    return "'" + v.replace("'", "''") + "'"


def _d(d: date) -> str:
    return "'" + d.isoformat() + "'"


def _num_or_null(x: Optional[float]) -> str:
    """Format numeric as plain string without scientific notation and no trailing zeros.

    Examples:
      17   -> '17'
      12.5 -> '12.5'
      12.0 -> '12'
    """
    if x is None:
        return "NULL"
    d = Decimal(str(x)).quantize(Decimal('0.01'), rounding=ROUND_HALF_UP)
    # Use fixed-point format to avoid scientific notation, then trim.
    s = format(d, 'f')
    if '.' in s:
        s = s.rstrip('0').rstrip('.')
    if s == '-0':
        s = '0'
    return s


def _int_or_null(x: Optional[int]) -> str:
    if x is None:
        return "NULL"
    return str(int(x))
