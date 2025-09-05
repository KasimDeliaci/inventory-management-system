#!/usr/bin/env python3
"""
Generate seed SQL (v0) from world.yaml without requiring CLI args.

Defaults:
  - config: scripts/data-generation/world.yaml (next to this file)
  - outdir: scripts/data-generation/out (creates subfolder sql/)

Optional overrides via CLI still supported:
  python scripts/data-generation/generate.py --config path/to/world.yaml --outdir path/to/out
"""

import argparse
import os
from pathlib import Path
from datetime import datetime

import yaml

# Defaults (edit here if you prefer code-level changes)
BASE_DIR = Path(__file__).resolve().parent
DEFAULT_CONFIG = BASE_DIR / 'world.yaml'
DEFAULT_OUTDIR = BASE_DIR / 'out'

import sys
# Allow local module imports from this folder when running as a script
sys.path.append(str(BASE_DIR))
from calendar_events import build_calendar, write_calendar
from campaigns import (
    generate_product_campaigns,
    generate_customer_offers,
    emit_campaigns_sql,
    emit_campaign_products_sql,
    emit_customer_offers_sql,
)


def sql_str(value: str) -> str:
    """Escape single quotes for SQL and wrap in quotes."""
    return "'" + value.replace("'", "''") + "'"


def emit_products(products):
    lines = []
    for p in products:
        cols = [
            'product_id', 'product_name', 'category', 'unit_of_measure',
            'safety_stock', 'reorder_point', 'current_price'
        ]
        vals = [
            str(int(p['id'])),
            sql_str(p['name']),
            sql_str(p['category']),
            sql_str(p['uom']),
            str(float(p['safety_stock'])),
            str(float(p['reorder_point'])),
            str(float(p['current_price']))
        ]
        lines.append(
            f"INSERT INTO products({', '.join(cols)}) VALUES ({', '.join(vals)});"
        )
    return lines


def emit_suppliers(suppliers):
    lines = []
    for s in suppliers:
        cols = ['supplier_id', 'supplier_name', 'email', 'phone', 'city']
        vals = [
            str(int(s['id'])),
            sql_str(s['name']),
            sql_str(s['email']),
            sql_str(s['phone']),
            sql_str(s['city'])
        ]
        lines.append(
            f"INSERT INTO suppliers({', '.join(cols)}) VALUES ({', '.join(vals)});"
        )
    return lines


def emit_customers(customers):
    lines = []
    for c in customers:
        cols = [
            'customer_id', 'customer_name', 'customer_segment', 'email', 'phone', 'city'
        ]
        vals = [
            str(int(c['id'])),
            sql_str(c['name']),
            sql_str(c['segment']),  # enum literal
            sql_str(c['email']),
            sql_str(c['phone']),
            sql_str(c['city'])
        ]
        lines.append(
            f"INSERT INTO customers({', '.join(cols)}) VALUES ({', '.join(vals)});"
        )
    return lines


def emit_product_suppliers(links):
    lines = []
    for l in links:
        cols = [
            'product_id', 'supplier_id', 'min_order_quantity', 'is_preferred', 'active'
        ]
        vals = [
            str(int(l['product_id'])),
            str(int(l['supplier_id'])),
            str(float(l['min_order_quantity'])),
            'TRUE' if l.get('is_preferred', False) else 'FALSE',
            'TRUE' if l.get('active', True) else 'FALSE'
        ]
        lines.append(
            f"INSERT INTO product_suppliers({', '.join(cols)}) VALUES ({', '.join(vals)});"
        )
    return lines


def main():
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument('--config', default=None, help='Path to world.yaml (optional)')
    parser.add_argument('--outdir', default=None, help='Output directory (optional)')
    args = parser.parse_args()

    config_path = Path(args.config) if args.config else DEFAULT_CONFIG
    outdir = Path(args.outdir) if args.outdir else DEFAULT_OUTDIR

    with open(config_path, 'r', encoding='utf-8') as f:
        world = yaml.safe_load(f)

    # Prepare output dirs with subfolders for future artifacts
    out_sql_dir = outdir / 'sql'
    out_sql_dir.mkdir(parents=True, exist_ok=True)

    ts = datetime.utcnow().isoformat() + 'Z'
    header = [
        f"-- Generated from {config_path.name} at {ts}",
        f"-- World: {world.get('meta', {}).get('name', '')}"
    ]

    sql_lines = []
    sql_lines.extend(header)
    sql_lines.append('-- Master Data: Products')
    sql_lines.extend(emit_products(world.get('products', [])))
    sql_lines.append('')
    sql_lines.append('-- Master Data: Suppliers')
    sql_lines.extend(emit_suppliers(world.get('suppliers', [])))
    sql_lines.append('')
    sql_lines.append('-- Master Data: Customers')
    sql_lines.extend(emit_customers(world.get('customers', [])))
    sql_lines.append('')
    sql_lines.append('-- Links: Product-Suppliers')
    sql_lines.extend(emit_product_suppliers(world.get('product_suppliers', [])))
    sql_lines.append('')

    out_sql = out_sql_dir / '00_master_seed.sql'
    with open(out_sql, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_lines))

    print(f"Wrote {out_sql}")

    # Calendar (phase v1): official holidays, weekends, Ramadan, special days
    cal_df = build_calendar(world.get('meta', {}))
    cal_path = write_calendar(cal_df, outdir)
    print(f"Wrote {cal_path}")

    # Campaigns & Offers (phase v2): generate consistent, non-overlapping schedules
    campaigns, assigns = generate_product_campaigns(world, cal_df)
    offers = generate_customer_offers(world)

    sql_campaigns_path = out_sql_dir / '10_campaigns.sql'
    with open(sql_campaigns_path, 'w', encoding='utf-8') as f:
        f.write('-- Campaigns\n')
        f.write('\n'.join(emit_campaigns_sql(campaigns)))
        f.write('\n\n-- Campaign→Product assignments\n')
        f.write('\n'.join(emit_campaign_products_sql(assigns)))
        f.write('\n\n-- Customer Special Offers\n')
        f.write('\n'.join(emit_customer_offers_sql(offers)))
        f.write('\n')
    print(f"Wrote {sql_campaigns_path}")

    # Also export CSVs for analysis
    camp_dir = outdir / 'campaigns'
    camp_dir.mkdir(parents=True, exist_ok=True)
    import pandas as pd
    pd.DataFrame([
        {
            'campaignId': c.campaign_id,
            'campaignName': c.campaign_name,
            'campaignType': c.campaign_type,
            'startDate': c.start_date,
            'endDate': c.end_date,
            'discountPercentage': c.discount_percentage,
            'buyQty': c.buy_qty,
            'getQty': c.get_qty,
        } for c in campaigns
    ]).to_csv(camp_dir / 'product_campaigns.csv', index=False, date_format='%Y-%m-%d')

    pd.DataFrame([
        {
            'specialOfferId': o.special_offer_id,
            'customerId': o.customer_id,
            'percentOff': o.percent_off,
            'startDate': o.start_date,
            'endDate': o.end_date,
        } for o in offers
    ]).to_csv(camp_dir / 'customer_offers.csv', index=False, date_format='%Y-%m-%d')
    print(f"Wrote {(camp_dir / 'product_campaigns.csv')} and {(camp_dir / 'customer_offers.csv')}")


if __name__ == '__main__':
    main()
