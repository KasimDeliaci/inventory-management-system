from __future__ import annotations

import math
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd


@dataclass
class ProductProfile:
    base_mean: float
    weekend_mult: float = 1.0
    summer_mult: float = 1.0
    winter_mult: float = 1.0
    spring_mult: float = 1.0
    autumn_mult: float = 1.0
    promo_strength: float = 1.0  # multiplier per 100% off (scaled by percent/100)
    dispersion: float = 8.0       # Negative binomial shape (larger = less variance)
    # Event multipliers
    valentine_mult: float = 1.0
    mothers_day_mult: float = 1.0
    teachers_day_mult: float = 1.0
    back_to_school_mult: float = 1.0
    ramadan_mult: float = 1.0
    eid_fitr_mult: float = 1.0
    eid_adha_mult: float = 1.0
    national_flag_day_mult: float = 1.0  # for flag product on national days
    # Correlated latent factor amplitude (AR(1) weather/occasion effect)
    latent_amp: float = 0.15


def _default_profiles(world: Dict) -> Dict[int, ProductProfile]:
    # Reasonable defaults for the 12 archetypes in world.yaml
    profiles: Dict[int, ProductProfile] = {
        1001: ProductProfile(base_mean=0.6, summer_mult=2.2, weekend_mult=1.05, promo_strength=1.1, dispersion=6, latent_amp=0.35),  # Sunscreen
        1002: ProductProfile(base_mean=0.15, summer_mult=3.0, weekend_mult=1.0, promo_strength=1.6, dispersion=4, latent_amp=0.40),   # AC
        1003: ProductProfile(base_mean=0.08, winter_mult=2.5, weekend_mult=1.15, promo_strength=1.2, dispersion=6, latent_amp=0.30),  # Ski board
        1004: ProductProfile(base_mean=0.4,  winter_mult=1.6, weekend_mult=1.0,  promo_strength=1.0, dispersion=8, latent_amp=0.15),  # Thermo flask
        1005: ProductProfile(base_mean=0.5,  ramadan_mult=6.0, eid_fitr_mult=1.6, weekend_mult=1.0, promo_strength=0.8, dispersion=10, latent_amp=0.4),  # Dates
        1006: ProductProfile(base_mean=1.4, valentine_mult=2.1, mothers_day_mult=1.6, teachers_day_mult=1.2, weekend_mult=1.15, promo_strength=1.3, dispersion=10, latent_amp=0.20),  # Chocolates
        1007: ProductProfile(base_mean=0.5, valentine_mult=1.8, mothers_day_mult=2.0, weekend_mult=1.05, promo_strength=1.4, dispersion=8, latent_amp=0.15),  # Perfume
        1008: ProductProfile(base_mean=6.0, weekend_mult=1.4, promo_strength=1.5, dispersion=12, latent_amp=0.20),  # Potato chips
        1009: ProductProfile(base_mean=4.0, weekend_mult=1.6, promo_strength=1.4, dispersion=12, latent_amp=0.25),  # Energy drink
        1010: ProductProfile(base_mean=0.9, back_to_school_mult=4.0, teachers_day_mult=1.2, weekend_mult=1.0, promo_strength=0.9, dispersion=10, latent_amp=0.30),  # Notebook
        1011: ProductProfile(base_mean=0.06, national_flag_day_mult=6.0, weekend_mult=1.0, promo_strength=0.8, dispersion=6, latent_amp=0.05),  # Turkish Flag
        1012: ProductProfile(base_mean=3.2, winter_mult=1.2, weekend_mult=1.05, promo_strength=0.8, dispersion=14, latent_amp=0.10),  # Tea
    }
    return profiles


def _campaign_daily_percent(product_id: int, day: date, campaigns_df: pd.DataFrame) -> float:
    """Return an equivalent percent-off for the product on the given day.
    DISCOUNT uses discountPercentage; BXGY uses get/(buy+get) * 100.
    If multiple overlapping were present (we avoid), we pick the max.
    """
    if campaigns_df is None or campaigns_df.empty:
        return 0.0
    rows = campaigns_df[campaigns_df['productId'] == product_id]
    if rows.empty:
        return 0.0
    # Filter by date inclusively
    active = rows[(rows['startDate'] <= pd.Timestamp(day)) & (rows['endDate'] >= pd.Timestamp(day))]
    if active.empty:
        return 0.0
    percents: List[float] = []
    for _, r in active.iterrows():
        if r['campaignType'] == 'DISCOUNT' and pd.notna(r['discountPercentage']):
            percents.append(float(r['discountPercentage']))
        elif r['campaignType'] == 'BXGY_SAME_PRODUCT' and pd.notna(r['buyQty']) and pd.notna(r['getQty']) and r['buyQty'] > 0:
            percents.append(100.0 * float(r['getQty']) / float(r['buyQty'] + r['getQty']))
    return max(percents) if percents else 0.0


def _national_flag_day(holiday_name: str) -> bool:
    if not holiday_name:
        return False
    n = holiday_name.lower()
    # Check for major national days that drive flag demand
    keywords = [
        'republic day',          # Oct 29
        'victory day',           # Aug 30
        'youth and sports',      # May 19
        "children's day",       # Apr 23
        'democracy and national unity',  # Jul 15
    ]
    return any(k in n for k in keywords)


def _season_mult_from_row(row: pd.Series, profile: ProductProfile) -> float:
    season = row['season']
    if season == 'summer':
        return profile.summer_mult
    if season == 'winter':
        return profile.winter_mult
    if season == 'spring':
        return profile.spring_mult
    if season == 'autumn':
        return profile.autumn_mult
    return 1.0


def _events_mult_from_row(row: pd.Series, profile: ProductProfile, product_id: int) -> float:
    m = 1.0
    if row['is_valentines']:
        m *= profile.valentine_mult
    if row['is_mothers_day']:
        m *= profile.mothers_day_mult
    if row['is_teachers_day']:
        m *= profile.teachers_day_mult
    if row['is_back_to_school']:
        m *= profile.back_to_school_mult
    if row['is_ramadan']:
        m *= profile.ramadan_mult
    if row['is_eid_fitr']:
        m *= profile.eid_fitr_mult
    if row['is_eid_adha']:
        m *= profile.eid_adha_mult
    # National flag specific
    if product_id == 1011 and _national_flag_day(row.get('holiday_names', '')):
        m *= profile.national_flag_day_mult
    return m


def _promo_mult(percent_off: float, promo_strength: float) -> float:
    # Simple linear response: multiplier = 1 + promo_strength * (percent/100)
    if percent_off <= 0:
        return 1.0
    return 1.0 + promo_strength * (percent_off / 100.0)


def _ar1(n: int, rho: float, sigma: float, rng: np.random.Generator) -> List[float]:
    vals = [0.0] * n
    if n == 0:
        return vals
    # Stationary variance of AR(1): sigma_eps^2 / (1 - rho^2)
    eps = rng.normal(loc=0.0, scale=sigma, size=n)
    vals[0] = eps[0]
    for t in range(1, n):
        vals[t] = rho * vals[t - 1] + eps[t]
    return vals



def _offer_pressure_by_date(world: Dict, calendar_df: pd.DataFrame, offers_df: Optional[pd.DataFrame]) -> Dict[date, float]:
    """Compute a weighted daily average percentOff across customers with active offers.

    Weights reflect segment order propensity and weekend multiplier, so the measure
    captures both breadth and strength of offers on a given day.
    Returns mapping date -> effective percent (0..100).
    """
    if offers_df is None or offers_df.empty:
        return {}

    # Ensure date types
    cal = calendar_df[['date', 'is_weekend']].copy()
    cal['date'] = pd.to_datetime(cal['date']).dt.date
    of = offers_df.copy()
    if 'startDate' in of.columns:
        of['startDate'] = pd.to_datetime(of['startDate']).dt.date
    if 'endDate' in of.columns:
        of['endDate'] = pd.to_datetime(of['endDate']).dt.date

    # Customer segments
    customers = world.get('customers', [])
    cust_seg: Dict[int, str] = {int(c['id']): str(c['segment']) for c in customers}
    policy = world.get('order_policy', {})
    base_rate = policy.get('base_rate_per_segment', {})
    weekend_mult = policy.get('weekend_mult_per_segment', {})

    result: Dict[date, float] = {}
    for _, row in cal.iterrows():
        d = row['date']
        is_wknd = bool(row['is_weekend'])
        # Total mass across all customers
        total_mass = 0.0
        active_weighted_pct = 0.0
        # Build a quick view of active offers for the day
        active = of[(of['startDate'] <= d) & (of['endDate'] >= d)]
        # Index by customer for fast lookup
        active_by_cust = {int(r['customerId']): float(r['percentOff']) for _, r in active.iterrows()}

        for cid, seg in cust_seg.items():
            w = float(base_rate.get(seg, 0.2))
            w *= float(weekend_mult.get(seg, 1.0)) if is_wknd else 1.0
            total_mass += w
            pct = active_by_cust.get(cid)
            if pct is not None:
                active_weighted_pct += w * pct

        eff = (active_weighted_pct / total_mass) if total_mass > 0 else 0.0
        result[d] = eff

    return result

def _sample_nb(mean: float, dispersion: float, rng: np.random.Generator) -> int:
    """Sample Negative Binomial with given mean and shape k (dispersion).

    Parameterization: variance = mean + mean^2 / k
    n = k, p = k / (k + mean)
    """
    if mean <= 0:
        return 0
    k = max(1e-6, float(dispersion))
    p = k / (k + mean)
    # numpy expects number of failures n and probability of success p
    return int(rng.negative_binomial(k, p))


def _ema(series: List[float], alpha: float) -> List[float]:
    if not series:
        return []
    s: List[float] = []
    prev = float(series[0])
    s.append(prev)
    a = float(alpha)
    for x in series[1:]:
        prev = a * float(x) + (1.0 - a) * prev
        s.append(prev)
    return s



def generate_demand(world: Dict, calendar_df: pd.DataFrame, product_campaigns_df: Optional[pd.DataFrame], offers_df: Optional[pd.DataFrame], product_ids: List[int], outdir) -> List[str]:
    profiles = _default_profiles(world)
    meta = world.get('meta', {})
    seed = int(meta.get('seed', 42))
    demand_scale = float(meta.get('demand_scale', 1.0))  # global scaling knob
    smooth_alpha = float(meta.get('smooth_alpha', 0.3))  # 0..1; lower = smoother (EMA)
    dispersion_scale = float(meta.get('dispersion_scale', 1.5))  # >1 increases k, reducing variance
    latent_rho = float(meta.get('latent_rho', 0.6))
    latent_sigma = float(meta.get('latent_sigma', 0.15))
    offer_alpha = float(meta.get('offer_effect_alpha', 0.6))
    offer_cap_pct = float(meta.get('offer_effect_cap_pct', 30.0))
    promo_alpha = float(meta.get('promo_effect_alpha', 1.0))

    # Precompute daily offer pressure (effective average percent-off across the customer base)
    offer_pressure = _offer_pressure_by_date(world, calendar_df, offers_df)


    start = pd.to_datetime(meta['start_date']).date()
    end = pd.to_datetime(meta['end_date']).date()
    dates = pd.date_range(start, end, freq='D')

    out_paths: List[str] = []
    out_demand_dir = outdir / 'datasets' / 'demand'
    out_demand_dir.mkdir(parents=True, exist_ok=True)

    # Ensure date typed column in calendar for joins
    cal = calendar_df.copy()
    cal['date'] = pd.to_datetime(cal['date']).dt.date

    for pid in product_ids:
        profile = profiles.get(pid, ProductProfile(base_mean=2.0, dispersion=10))
        rng = np.random.default_rng(seed + pid)

        # First compute the unsmoothed expected means and promo pct per day
        base_means: List[float] = []
        promo_pcts: List[float] = []
        dates_list: List[date] = []
        for _, crow in cal.iterrows():
            mu = profile.base_mean
            mu *= _season_mult_from_row(crow, profile)
            if crow['is_weekend']:
                mu *= profile.weekend_mult
            mu *= _events_mult_from_row(crow, profile, pid)
            pct = _campaign_daily_percent(pid, crow['date'], product_campaigns_df) if product_campaigns_df is not None else 0.0
            # Apply product campaign multiplier with global promo scaler
            mu *= _promo_mult(pct, profile.promo_strength * promo_alpha)

            # Offer multiplier: 1 + alpha * (effective offer percent / 100), capped
            eff_offer_pct = float(offer_pressure.get(crow['date'], 0.0))
            m_offer = 1.0 + offer_alpha * (eff_offer_pct / 100.0)
            cap = 1.0 + (offer_cap_pct / 100.0)
            if m_offer > cap:
                m_offer = cap
            mu *= m_offer
            base_means.append(mu)
            promo_pcts.append(round(pct, 2))
            dates_list.append(crow['date'])

        # Apply EMA smoothing to the mean signal to reduce day-to-day jitter
        if 0.0 < smooth_alpha < 1.0:
            smoothed = _ema(base_means, smooth_alpha)
        else:
            smoothed = base_means

        # Apply correlated latent factor (e.g., weather) with product-specific amplitude
        lat = _ar1(len(smoothed), latent_rho, latent_sigma, rng)

        rows = []
        k = max(1e-6, float(profile.dispersion) * max(0.1, dispersion_scale))

        # Post-sample soft-shrink config
        sample_soft = bool(meta.get('sample_soft_shrink_on_quiet_days', True))
        soft_lambda = float(meta.get('sample_soft_shrink_lambda', 0.5))
        win = int(meta.get('clamp_window_days', 7))
        band_pct = float(meta.get('clamp_band_pct', 0.10))
        use_same_dow = bool(meta.get('clamp_use_same_dow', True))
        max_campaign_for_clamp = float(meta.get('clamp_max_campaign_pct_for_clamp', 0.0))
        max_offer_for_clamp = float(meta.get('clamp_max_offer_pressure_pct_for_clamp', 0.0))
        skip_specials = bool(meta.get('clamp_skip_specials', False))

        # Precompute post-latent means (before scaling) to build same-DOW references
        mu_lat_pre = [float(mu * np.exp(profile.latent_amp * l)) for mu, l in zip(smoothed, lat)]

        for i, (mu_pre, pct, d) in enumerate(zip(mu_lat_pre, promo_pcts, dates_list)):
            mu_scaled = mu_pre * demand_scale
            y = _sample_nb(mu_scaled, k, rng)

            if sample_soft:
                # Quiet day check
                is_quiet = True
                if pct is not None and float(pct) > max_campaign_for_clamp:
                    is_quiet = False
                eff_offer_pct = float(offer_pressure.get(d, 0.0))
                if eff_offer_pct > max_offer_for_clamp:
                    is_quiet = False
                if skip_specials:
                    crow = cal.iloc[i]
                    special_flags = [
                        bool(crow.get('is_official_holiday', False)),
                        bool(crow.get('is_ramadan', False)),
                        bool(crow.get('is_eid_fitr', False)),
                        bool(crow.get('is_eid_adha', False)),
                        bool(crow.get('is_valentines', False)),
                        bool(crow.get('is_mothers_day', False)),
                        bool(crow.get('is_teachers_day', False)),
                        bool(crow.get('is_ataturk_memorial', False)),
                        bool(crow.get('is_black_friday', False)),
                        bool(crow.get('is_back_to_school', False)),
                    ]
                    if any(special_flags):
                        is_quiet = False

                if is_quiet:
                    # Build same-DOW reference over post-latent means
                    ref_vals: List[float] = []
                    if use_same_dow:
                        target_dow = pd.Timestamp(d).weekday()
                        j = i - 1
                        while j >= 0 and len(ref_vals) < win:
                            if pd.Timestamp(dates_list[j]).weekday() == target_dow:
                                v = mu_lat_pre[j]
                                if v > 0:
                                    ref_vals.append(v)
                            j -= 1
                    else:
                        for j in range(max(0, i - win), i):
                            v = mu_lat_pre[j]
                            if v > 0:
                                ref_vals.append(v)

                    if ref_vals:
                        logs = np.log(np.array(ref_vals, dtype=float))
                        ref = float(np.exp(np.median(logs))) * demand_scale
                        lower = (1.0 - band_pct) * ref
                        upper = (1.0 + band_pct) * ref
                        if y < lower or y > upper:
                            y = int(round(ref + soft_lambda * (y - ref)))
                            if y < 0:
                                y = 0

            rows.append({'date': d, 'productId': pid, 'demand': y, 'promoPct': pct})

        df = pd.DataFrame(rows)
        df.sort_values('date', inplace=True)
        out_path = out_demand_dir / f'product_{pid}.csv'
        df.to_csv(out_path, index=False, date_format='%Y-%m-%d')
        out_paths.append(str(out_path))

    # If 'all' selected, also write a combined file for convenience
    if len(product_ids) > 1:
        combined = []
        for p in product_ids:
            combined.append(pd.read_csv(out_demand_dir / f'product_{p}.csv'))
        pd.concat(combined, ignore_index=True).to_csv(out_demand_dir / 'all_products.csv', index=False)

    return out_paths
