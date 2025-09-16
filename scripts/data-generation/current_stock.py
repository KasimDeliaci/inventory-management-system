from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import math
import random
import pandas as pd


COUNTABLE_UOMS = {"adet", "koli", "paket", "çuval", "şişe"}


@dataclass
class ProductCfg:
    product_id: int
    uom: str
    safety_stock: float
    reorder_point: float


def _products_from_world(world: Dict) -> Dict[int, ProductCfg]:
    out: Dict[int, ProductCfg] = {}
    for p in world.get("products", []):
        pid = int(p["id"])
        out[pid] = ProductCfg(
            product_id=pid,
            uom=str(p.get("uom", "")),
            safety_stock=float(p.get("safety_stock", 0)),
            reorder_point=float(p.get("reorder_point", 0)),
        )
    return out


def _avg_recent_sales(sales_df: Optional[pd.DataFrame], pid: int, days: int = 14) -> float:
    if sales_df is None or sales_df.empty:
        return 0.0
    dff = sales_df[sales_df["productId"] == pid].copy()
    if dff.empty:
        return 0.0
    dff = dff.sort_values("date")
    tail = dff.tail(days)
    if tail.empty:
        return 0.0
    return float(tail["observedSales"].mean())


def _round_qty(uom: str, qty: float) -> float:
    if (uom or "").strip().lower() in COUNTABLE_UOMS:
        return float(max(0, int(round(qty))))
    return float(round(qty, 3))


def _round_up_to_moq(uom: str, qty: float, moq: float) -> float:
    if moq is None or moq <= 0:
        return _round_qty(uom, qty)
    if (uom or "").strip().lower() in COUNTABLE_UOMS:
        # integer multiples
        base = max(0, int(math.ceil(qty)))
        multiple = max(1, int(math.ceil(base / max(1, int(round(moq))))))
        return float(multiple * int(round(moq)))
    # non-countable: ceil to 3 decimals on multiples
    k = math.ceil(qty / moq)
    return float(round(k * moq, 3))


def _preferred_moq_by_product(world: Dict) -> Dict[int, float]:
    out: Dict[int, float] = {}
    links = world.get("product_suppliers", []) or []
    by_pid: Dict[int, List[Dict]] = {}
    for l in links:
        by_pid.setdefault(int(l["product_id"]), []).append(l)
    for pid, lst in by_pid.items():
        # prefer is_preferred, else smallest moq among active links
        preferred = [l for l in lst if l.get("is_preferred")]
        if preferred:
            out[pid] = float(preferred[0].get("min_order_quantity", 0.0))
        else:
            moqs = [float(l.get("min_order_quantity", 0.0)) for l in lst if l.get("active", True)]
            out[pid] = min(moqs) if moqs else 0.0
    return out


def generate_current_stock(world: Dict, outdir: Path, product_day_sales_csv: Optional[Path] = None) -> Tuple[str, str]:
    """Generate a plausible current_stock snapshot for all products.

    Strategy:
      - Use last 14-day average observed sales as daily demand proxy (0 if missing).
      - Use policy.target_days_of_cover (default 21) and safetyStock to size on-hand.
      - Reserve a small share (2–6%) to simulate allocations; ensure non-negative.
      - Round to MOQ-like buckets if countable (we approximate MOQ per product: reorder_point/3 or 1 for countables).
    Outputs:
      - out/sql/15_current_stock.sql with UPSERTs into current_stock
      - out/inventory/current_stock.csv for inspection
    """
    products = _products_from_world(world)

    meta = world.get("meta", {})
    as_of_str = str(meta.get("end_date"))
    try:
        as_of = datetime.fromisoformat(as_of_str)
    except Exception:
        as_of = datetime.utcnow()

    policy = world.get("policy", {})
    reorder_policy = policy.get("reorder_strategy", {})
    target_doc = int(reorder_policy.get("target_days_of_cover", 21))

    sales_df: Optional[pd.DataFrame] = None
    if product_day_sales_csv and Path(product_day_sales_csv).exists():
        sales_df = pd.read_csv(product_day_sales_csv, parse_dates=["date"])  # columns: date, productId, observedSales, offerActiveShare

    rows: List[Dict] = []
    rng = random.Random(42)

    moq_map = _preferred_moq_by_product(world)

    for pid, cfg in products.items():
        avg_sales = _avg_recent_sales(sales_df, pid, days=14)
        # baseline on-hand: avg_sales * target_doc + safety
        base_on_hand = avg_sales * max(7, target_doc) + cfg.safety_stock
        # add mild noise (±10%)
        noise = 1.0 + rng.uniform(-0.1, 0.1)
        on_hand_raw = max(cfg.reorder_point, base_on_hand * noise)

        # approximate MOQ: for countables, bucket to reorder_point/3 at minimum 1
        uom = cfg.uom
        preferred_moq = float(moq_map.get(pid, 0.0))
        if preferred_moq <= 0:
            approx_moq = 1.0 if (uom or "").strip().lower() in COUNTABLE_UOMS else max(0.1, round(cfg.reorder_point / 10.0, 3))
        else:
            approx_moq = preferred_moq
        on_hand = _round_up_to_moq(uom, on_hand_raw, approx_moq)

        # reserved 2–6% of on_hand
        reserved = _round_qty(uom, on_hand * rng.uniform(0.02, 0.06))
        if reserved > on_hand:
            reserved = _round_qty(uom, on_hand * 0.02)

        rows.append({
            "productId": pid,
            "quantityOnHand": on_hand,
            "quantityReserved": reserved,
            "lastUpdated": (as_of + timedelta(hours=12)).isoformat() + "Z",
        })

    df = pd.DataFrame(rows)
    inv_dir = outdir / "inventory"
    inv_dir.mkdir(parents=True, exist_ok=True)
    csv_path = inv_dir / "current_stock.csv"
    df.to_csv(csv_path, index=False)

    # Emit SQL upserts
    sql_dir = outdir / "sql"
    sql_dir.mkdir(parents=True, exist_ok=True)
    sql_path = sql_dir / "15_current_stock.sql"
    lines: List[str] = ["-- Current Stock snapshot (dummy, generated)"]
    for r in rows:
        pid = int(r["productId"])
        qoh = float(r["quantityOnHand"]) if r["quantityOnHand"] is not None else 0.0
        qrs = float(r["quantityReserved"]) if r["quantityReserved"] is not None else 0.0
        lu = r["lastUpdated"]
        lines.append(
            "INSERT INTO current_stock(product_id, quantity_on_hand, quantity_reserved, last_updated) "
            f"VALUES ({pid}, {qoh:.3f}, {qrs:.3f}, '{lu}') "
            "ON CONFLICT (product_id) DO UPDATE SET "
            f"quantity_on_hand = EXCLUDED.quantity_on_hand, quantity_reserved = EXCLUDED.quantity_reserved, last_updated = EXCLUDED.last_updated;"
        )
    with open(sql_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")

    return str(sql_path), str(csv_path)
