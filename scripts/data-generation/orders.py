from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd


COUNTABLE_UOMS = {"adet", "koli", "paket", "çuval", "şişe"}


@dataclass
class SOHeader:
    sales_order_id: int
    customer_id: int
    order_date: date
    delivery_date: date
    status: str
    delivered_at: datetime
    customer_special_offer_id: Optional[int]
    customer_discount_pct_applied: Optional[float]


@dataclass
class SOLine:
    sales_order_item_id: int
    sales_order_id: int
    product_id: int
    quantity: float
    unit_price: float
    discount_percentage: float
    campaign_id: Optional[int]


def _rng(seed: int) -> random.Random:
    return random.Random(seed)


def _stack_discounts(prod_pct: float, cust_pct: float) -> float:
    combined = 1.0 - (1.0 - prod_pct / 100.0) * (1.0 - cust_pct / 100.0)
    return round(combined * 100.0, 2)


def _active_offer_for_customer(offers_df: pd.DataFrame, cid: int, d: date) -> Tuple[Optional[int], float]:
    if offers_df is None or offers_df.empty:
        return None, 0.0
    sub = offers_df[(offers_df['customerId'] == cid) & (offers_df['startDate'] <= pd.Timestamp(d)) & (offers_df['endDate'] >= pd.Timestamp(d))]
    if sub.empty:
        return None, 0.0
    r = sub.iloc[0]
    return int(r['specialOfferId']), float(r['percentOff'])


def _active_campaign_for_product(camp_df: pd.DataFrame, pid: int, d: date) -> Tuple[Optional[int], float]:
    if camp_df is None or camp_df.empty:
        return None, 0.0
    sub = camp_df[(camp_df['productId'] == pid) & (camp_df['startDate'] <= pd.Timestamp(d)) & (camp_df['endDate'] >= pd.Timestamp(d))]
    if sub.empty:
        return None, 0.0
    best_row = None
    best_pct = -1.0
    for _, r in sub.iterrows():
        if r['campaignType'] == 'DISCOUNT' and not pd.isna(r['discountPercentage']):
            pct = float(r['discountPercentage'])
        elif r['campaignType'] == 'BXGY_SAME_PRODUCT' and not pd.isna(r['buyQty']) and not pd.isna(r['getQty']) and r['buyQty'] > 0:
            pct = 100.0 * float(r['getQty']) / float(r['buyQty'] + r['getQty'])
        else:
            pct = 0.0
        if pct > best_pct:
            best_pct = pct
            best_row = r
    if best_row is None:
        return None, 0.0
    return int(best_row['campaignId']), best_pct


def _choose_basket_size(policy: Dict, rng: random.Random) -> int:
    probs = policy.get('basket_size_probs', [0.6, 0.3, 0.1])
    support = [1, 2, 3]
    x = rng.random()
    acc = 0.0
    for size, p in zip(support, probs):
        acc += p
        if x <= acc:
            return size
    return support[-1]


def _product_weights_for_segment(world: Dict, segment: str, policy: Dict) -> List[Tuple[int, float]]:
    aff = policy.get('segment_category_affinity', {})
    seg_aff = aff.get(segment, {})
    weights = []
    for p in world.get('products', []):
        base = 1.0
        cat = str(p['category'])
        w = float(seg_aff.get(cat, base))
        weights.append((int(p['id']), w))
    s = sum(w for _, w in weights)
    if s <= 0:
        return [(pid, 1.0) for pid, _ in weights]
    return [(pid, w / s) for pid, w in weights]


def _sample_products_without_replacement(weights: List[Tuple[int, float]], k: int, rng: random.Random) -> List[int]:
    chosen: List[int] = []
    pool = list(weights)
    for _ in range(min(k, len(pool))):
        r = rng.random()
        acc = 0.0
        idx = 0
        for i, (pid, w) in enumerate(pool):
            acc += w
            if r <= acc:
                idx = i
                break
        chosen_pid = pool[idx][0]
        chosen.append(chosen_pid)
        del pool[idx]
        s = sum(w for _, w in pool) or 1.0
        pool = [(pid, w / s) for pid, w in pool]
    return chosen


def _quantity_for_uom(uom: str, policy: Dict, rng: random.Random) -> float:
    if uom in COUNTABLE_UOMS:
        probs_map = policy.get('countable_qty_probs', {"1": 0.7, "2": 0.25, "3": 0.05})
        choices = [1, 2, 3]
        probs = [float(probs_map.get(str(c), 0.0)) for c in choices]
        s = sum(probs) or 1.0
        probs = [p / s for p in probs]
        x = rng.random()
        acc = 0.0
        for c, p in zip(choices, probs):
            acc += p
            if x <= acc:
                return float(c)
        return float(choices[-1])
    return float(policy.get('noncountable_default_qty', 1.0))


def generate_orders(world: Dict, calendar_df: pd.DataFrame, campaigns_df: pd.DataFrame, offers_df: pd.DataFrame, outdir: Path, demand_df: Optional[pd.DataFrame] = None):
    meta = world.get('meta', {})
    policy = world.get('order_policy', {})
    seed = int(meta.get('seed', 42))
    rng = _rng(seed + 101)

    products = world.get('products', [])
    customers = world.get('customers', [])
    by_pid = {int(p['id']): p for p in products}

    cal = calendar_df.copy()
    cal['date'] = pd.to_datetime(cal['date']).dt.date
    campaigns_df = campaigns_df.copy() if campaigns_df is not None else None
    if campaigns_df is not None:
        for col in ('startDate', 'endDate'):
            campaigns_df[col] = pd.to_datetime(campaigns_df[col])
    offers_df = offers_df.copy() if offers_df is not None else None
    if offers_df is not None:
        for col in ('startDate', 'endDate'):
            offers_df[col] = pd.to_datetime(offers_df[col])

    base_rate = policy.get('base_rate_per_segment', {})
    weekend_mult = policy.get('weekend_mult_per_segment', {})
    follow_demand = bool(policy.get('orders_follow_demand', False)) and demand_df is not None

    headers: List[SOHeader] = []
    lines: List[SOLine] = []
    product_day: Dict[Tuple[date, int], Dict[str, float]] = {}

    next_so_id = 60001
    next_line_id = 80001

    if follow_demand:
        # Normalize demand_df
        dd = demand_df.copy()
        if 'date' in dd.columns:
            dd['date'] = pd.to_datetime(dd['date']).dt.date
        dd = dd.groupby(['date', 'productId'], as_index=False)['demand'].sum()

        # Iterate per day
        for _, row in cal.iterrows():
            d = row['date']
            is_weekend = bool(row['is_weekend'])

            # Build customer pool with weights influenced by offers and segments
            cust_weights: List[Tuple[int, float]] = []
            for c in customers:
                cid = int(c['id'])
                seg = str(c['segment'])
                w = float(base_rate.get(seg, 0.2))
                w *= float(weekend_mult.get(seg, 1.0)) if is_weekend else 1.0
                offer_id, offer_pct = _active_offer_for_customer(offers_df, cid, d)
                uplift_alpha = float(policy.get('offer_uplift_alpha', 1.0))
                w *= (1.0 + uplift_alpha * (offer_pct / 100.0))
                cust_weights.append((cid, max(w, 0.0)))
            # Normalize weights
            total_w = sum(w for _, w in cust_weights) or 1.0
            cust_weights = [(cid, w / total_w) for cid, w in cust_weights]

            # Helper to sample a customer
            def pick_customer(rng_local: random.Random) -> int:
                x = rng_local.random()
                acc = 0.0
                for cid, w in cust_weights:
                    acc += w
                    if x <= acc:
                        return cid
                return cust_weights[-1][0]

            # Get product demands for the day
            day_rows = dd[dd['date'] == d]
            # Allocate lines per product to consume demand
            allocations: Dict[Tuple[int, int], float] = {}  # key=(cid, pid) -> qty
            offer_snapshot: Dict[int, Tuple[Optional[int], float]] = {}

            for _, pr in day_rows.iterrows():
                pid = int(pr['productId'])
                remaining = float(pr['demand'])
                if remaining <= 0:
                    continue
                uom = str(by_pid[pid]['uom'])
                # Allocate to customers until remaining is consumed
                while remaining > 1e-9:
                    cid = pick_customer(rng)
                    offer_id, offer_pct = _active_offer_for_customer(offers_df, cid, d)
                    offer_snapshot[cid] = (offer_id, offer_pct)
                    qty = _quantity_for_uom(uom, policy, rng)
                    if uom in COUNTABLE_UOMS:
                        qty = max(1.0, float(int(qty)))
                        if qty > remaining:
                            qty = remaining
                    else:
                        # Non-countable: cap to remaining for exact consumption
                        qty = min(qty, remaining)
                    allocations[(cid, pid)] = allocations.get((cid, pid), 0.0) + qty
                    remaining -= qty

            # Now pack allocations into orders per customer
            for cid in {cid for (cid, _) in allocations.keys()}:
                # Build product->qty map
                prod_qty: Dict[int, float] = {}
                for (cid2, pid), q in allocations.items():
                    if cid2 != cid:
                        continue
                    prod_qty[pid] = prod_qty.get(pid, 0.0) + q

                # Determine offer snapshot for header
                offer_id, offer_pct = offer_snapshot.get(cid, (None, 0.0))

                # Create orders until all products consumed
                while prod_qty:
                    order_date = d
                    delivery_date = d
                    delivered_at = datetime.combine(d, datetime.min.time()) + timedelta(hours=12)
                    header = SOHeader(
                        sales_order_id=next_so_id,
                        customer_id=cid,
                        order_date=order_date,
                        delivery_date=delivery_date,
                        status='DELIVERED',
                        delivered_at=delivered_at,
                        customer_special_offer_id=offer_id,
                        customer_discount_pct_applied=(round(offer_pct, 2) if offer_pct and offer_pct > 0 else None),
                    )
                    headers.append(header)

                    # Choose basket size limited by distinct products remaining
                    max_size = min(3, len(prod_qty))
                    size = _choose_basket_size(policy, rng)
                    size = min(size, max_size)
                    # Pick products for this order
                    prod_list = list(prod_qty.keys())
                    rng.shuffle(prod_list)
                    chosen = prod_list[:size]
                    for pid in chosen:
                        q = prod_qty.pop(pid)
                        prod = by_pid[pid]
                        unit_price = float(prod['current_price'])
                        camp_id, prod_pct = _active_campaign_for_product(campaigns_df, pid, d)
                        cust_pct = offer_pct or 0.0
                        disc_pct = _stack_discounts(prod_pct, cust_pct)
                        line = SOLine(
                            sales_order_item_id=next_line_id,
                            sales_order_id=next_so_id,
                            product_id=pid,
                            quantity=round(float(q), 3),
                            unit_price=unit_price,
                            discount_percentage=disc_pct,
                            campaign_id=camp_id,
                        )
                        lines.append(line)

                        key = (d, pid)
                        agg = product_day.setdefault(key, {"units": 0.0, "offer_units": 0.0})
                        agg["units"] += float(q)
                        if cust_pct > 0:
                            agg["offer_units"] += float(q)
                        next_line_id += 1

                    next_so_id += 1
    else:
        # Previous propensity-based approach
        for _, row in cal.iterrows():
            d = row['date']
            is_weekend = bool(row['is_weekend'])
            for c in customers:
                cid = int(c['id'])
                seg = str(c['segment'])
                lam = float(base_rate.get(seg, 0.2))
                lam *= float(weekend_mult.get(seg, 1.0)) if is_weekend else 1.0
                offer_id, offer_pct = _active_offer_for_customer(offers_df, cid, d)
                uplift_alpha = float(policy.get('offer_uplift_alpha', 1.0))
                lam *= (1.0 + uplift_alpha * (offer_pct / 100.0))

                k = np.random.default_rng(seed + cid + int(pd.Timestamp(d).toordinal())).poisson(lam)
                if k <= 0:
                    continue

                weights = _product_weights_for_segment(world, seg, policy)
                for _ in range(int(k)):
                    order_date = d
                    delivery_date = d
                    delivered_at = datetime.combine(d, datetime.min.time()) + timedelta(hours=12)
                    header = SOHeader(
                        sales_order_id=next_so_id,
                        customer_id=cid,
                        order_date=order_date,
                        delivery_date=delivery_date,
                        status='DELIVERED',
                        delivered_at=delivered_at,
                        customer_special_offer_id=offer_id,
                        customer_discount_pct_applied=(round(offer_pct, 2) if offer_pct > 0 else None),
                    )
                    headers.append(header)

                    basket_size = _choose_basket_size(policy, _rng(seed + next_so_id))
                    chosen_pids = _sample_products_without_replacement(weights, basket_size, _rng(seed + next_line_id))
                    for pid in chosen_pids:
                        prod = by_pid[pid]
                        uom = str(prod['uom'])
                        qty = _quantity_for_uom(uom, policy, rng)
                        unit_price = float(prod['current_price'])
                        camp_id, prod_pct = _active_campaign_for_product(campaigns_df, pid, d)
                        cust_pct = offer_pct
                        disc_pct = _stack_discounts(prod_pct, cust_pct)
                        line = SOLine(
                            sales_order_item_id=next_line_id,
                            sales_order_id=next_so_id,
                            product_id=pid,
                            quantity=round(qty, 3),
                            unit_price=unit_price,
                            discount_percentage=disc_pct,
                            campaign_id=camp_id,
                        )
                        lines.append(line)

                        key = (d, pid)
                        agg = product_day.setdefault(key, {"units": 0.0, "offer_units": 0.0})
                        agg["units"] += qty
                        if offer_pct > 0:
                            agg["offer_units"] += qty

                        next_line_id += 1

                    next_so_id += 1

    out_sql_dir = outdir / 'sql'
    out_sql_dir.mkdir(parents=True, exist_ok=True)
    so_sql = out_sql_dir / '20_sales_orders.sql'
    soi_sql = out_sql_dir / '21_sales_order_items.sql'

    with open(so_sql, 'w', encoding='utf-8') as f:
        f.write('-- Sales Orders (Phase A: delivered same-day, no stock)\n')
        for h in headers:
            cols = [
                'sales_order_id', 'customer_id', 'order_date', 'delivery_date', 'delivered_at', 'status',
                'customer_special_offer_id', 'customer_discount_pct_applied'
            ]
            vals = [
                str(h.sales_order_id),
                str(h.customer_id),
                f"'{h.order_date.isoformat()}'",
                f"'{h.delivery_date.isoformat()}'",
                f"'{h.delivered_at.isoformat()}Z'",
                "'DELIVERED'",
                ('NULL' if h.customer_special_offer_id is None else str(h.customer_special_offer_id)),
                ('NULL' if h.customer_discount_pct_applied is None else f"{h.customer_discount_pct_applied:.2f}"),
            ]
            f.write(f"INSERT INTO sales_orders({', '.join(cols)}) VALUES ({', '.join(vals)});\n")

    with open(soi_sql, 'w', encoding='utf-8') as f:
        f.write('-- Sales Order Items (Phase A)\n')
        for l in lines:
            cols = [
                'sales_order_item_id', 'sales_order_id', 'product_id', 'quantity', 'unit_price', 'discount_percentage', 'campaign_id'
            ]
            vals = [
                str(l.sales_order_item_id),
                str(l.sales_order_id),
                str(l.product_id),
                f"{l.quantity:.3f}",
                f"{l.unit_price:.2f}",
                f"{l.discount_percentage:.2f}",
                ('NULL' if l.campaign_id is None else str(l.campaign_id)),
            ]
            f.write(f"INSERT INTO sales_order_items({', '.join(cols)}) VALUES ({', '.join(vals)});\n")

    out_sales_dir = outdir / 'sales'
    out_sales_dir.mkdir(parents=True, exist_ok=True)

    orders_df = pd.DataFrame([
        {
            'salesOrderId': h.sales_order_id,
            'customerId': h.customer_id,
            'orderDate': h.order_date,
            'deliveryDate': h.delivery_date,
            'status': h.status,
            'deliveredAt': h.delivered_at,
            'customerSpecialOfferId': h.customer_special_offer_id,
            'customerDiscountPctApplied': h.customer_discount_pct_applied,
        } for h in headers
    ])
    # Ensure integer formatting for IDs (nullable Int64 keeps missing as empty)
    if 'customerSpecialOfferId' in orders_df.columns:
        orders_df['customerSpecialOfferId'] = pd.to_numeric(orders_df['customerSpecialOfferId'], errors='coerce').astype('Int64')
    orders_df.to_csv(out_sales_dir / 'orders.csv', index=False, date_format='%Y-%m-%d')

    items_df = pd.DataFrame([
        {
            'salesOrderItemId': l.sales_order_item_id,
            'salesOrderId': l.sales_order_id,
            'productId': l.product_id,
            'quantity': l.quantity,
            'unitPrice': l.unit_price,
            'discountPercentage': l.discount_percentage,
            'campaignId': l.campaign_id,
        } for l in lines
    ])
    if 'campaignId' in items_df.columns:
        items_df['campaignId'] = pd.to_numeric(items_df['campaignId'], errors='coerce').astype('Int64')
    items_df.to_csv(out_sales_dir / 'order_items.csv', index=False)

    rows = []
    for (d, pid), agg in sorted(product_day.items()):
        units = float(agg['units'])
        offer_share = (float(agg['offer_units']) / units) if units > 0 else 0.0
        rows.append({'date': d, 'productId': pid, 'observedSales': units, 'offerActiveShare': round(offer_share, 3)})
    prod_day_csv = outdir / 'datasets' / 'demand' / 'product_day_sales.csv'
    prod_day_csv.parent.mkdir(parents=True, exist_ok=True)
    pd.DataFrame(rows).to_csv(prod_day_csv, index=False, date_format='%Y-%m-%d')

    return str(so_sql), str(soi_sql), str(out_sales_dir / 'orders.csv'), str(out_sales_dir / 'order_items.csv'), str(prod_day_csv)
