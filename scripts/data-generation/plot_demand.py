#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd
import matplotlib.pyplot as plt


BASE_DIR = Path(__file__).resolve().parent
DEFAULT_OUTDIR = BASE_DIR / 'out'
DEFAULT_DATASETS_DIR = DEFAULT_OUTDIR / 'datasets' / 'demand'


def _load_csv(product_arg: str, datasets_dir: Path) -> tuple[pd.DataFrame, str]:
    """Load a demand CSV based on --product argument.

    Accepts an integer-like product id or a CSV filename/path.
    Returns (df, label) where label is used in plot titles.
    """
    # If numeric -> product_<id>.csv
    product_label = product_arg
    csv_path: Path
    if product_arg.isdigit():
        csv_path = datasets_dir / f'product_{int(product_arg)}.csv'
        product_label = f'product {int(product_arg)}'
    else:
        # Treat as filename; if not absolute, join datasets_dir
        p = Path(product_arg)
        csv_path = p if p.is_absolute() else (datasets_dir / p)
        product_label = csv_path.stem

    if not csv_path.exists():
        raise SystemExit(f"CSV not found: {csv_path}")

    df = pd.read_csv(csv_path, parse_dates=['date'])
    return df, product_label


def _ensure_dirs(outdir: Path):
    (outdir / 'plots' / 'monthly').mkdir(parents=True, exist_ok=True)
    (outdir / 'plots' / 'yearly').mkdir(parents=True, exist_ok=True)
    (outdir / 'plots' / 'all').mkdir(parents=True, exist_ok=True)


def plot_monthly(df: pd.DataFrame, product_label: str, outdir: Path, month: str | None):
    # Determine target month (YYYY-MM); default to latest month in data
    df = df.copy()
    df['year'] = df['date'].dt.year
    df['month'] = df['date'].dt.month
    df['day'] = df['date'].dt.day

    if month:
        try:
            y, m = month.split('-')
            year_i, month_i = int(y), int(m)
        except Exception:
            raise SystemExit("--month must be in YYYY-MM format")
    else:
        last = df['date'].dt.to_period('M').max()
        year_i, month_i = last.year, last.month

    dff = df[(df['year'] == year_i) and (df['month'] == month_i)] if False else df[(df['year'] == year_i) & (df['month'] == month_i)]
    series = dff.groupby('day')['demand'].sum()

    # Create aligned index 1..31
    idx = pd.Index(range(1, 32), name='day')
    series = series.reindex(idx)

    plt.figure(figsize=(10, 5))
    plt.plot(series.index, series.values, marker='o')
    plt.title(f"Monthly Demand – {product_label} – {year_i}-{month_i:02d}")
    plt.xlabel('Day of month')
    plt.ylabel('Units')
    plt.grid(True, alpha=0.3)
    plt.xlim(1, 31)
    plt.xticks(range(1, 32))
    out_path = outdir / 'plots' / 'monthly' / f"{product_label.replace(' ', '_')}_{year_i}-{month_i:02d}.png"
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()
    print(f"Wrote {out_path}")


def plot_yearly(df: pd.DataFrame, product_label: str, outdir: Path, year: int | None):
    df = df.copy()
    df['year'] = df['date'].dt.year
    df['month'] = df['date'].dt.month
    if year is None:
        year = int(df['year'].max())
    dff = df[df['year'] == year]
    series = dff.groupby('month')['demand'].sum().reindex(range(1, 13))

    plt.figure(figsize=(10, 5))
    plt.plot(series.index, series.values, marker='o')
    plt.title(f"Yearly Demand – {product_label} – {year}")
    plt.xlabel('Month')
    plt.ylabel('Units')
    plt.grid(True, alpha=0.3)
    plt.xlim(1, 12)
    plt.xticks(range(1, 13))
    out_path = outdir / 'plots' / 'yearly' / f"{product_label.replace(' ', '_')}_{year}.png"
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()
    print(f"Wrote {out_path}")


def plot_all_quarters(df: pd.DataFrame, product_label: str, outdir: Path):
    df = df.copy()
    df['quarter'] = df['date'].dt.to_period('Q')
    series = df.groupby('quarter')['demand'].sum().sort_index()
    x = [str(p) for p in series.index]
    y = series.values

    plt.figure(figsize=(12, 5))
    plt.plot(x, y, marker='o')
    plt.title(f"All-Time Demand by Quarter – {product_label}")
    plt.xlabel('Quarter (YYYYQx)')
    plt.ylabel('Units')
    plt.grid(True, alpha=0.3)
    plt.xticks(rotation=45, ha='right')
    out_path = outdir / 'plots' / 'all' / f"{product_label.replace(' ', '_')}_quarters.png"
    plt.tight_layout()
    plt.savefig(out_path)
    plt.close()
    print(f"Wrote {out_path}")


def main():
    parser = argparse.ArgumentParser(description='Plot demand timeseries.')
    parser.add_argument('--product', required=True, help='Product id (e.g., 1009) or CSV filename (e.g., product_1009.csv)')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--monthly', action='store_true', help='Plot a single month (x=1..31). Use --month YYYY-MM to select; defaults to latest month in data')
    group.add_argument('--yearly', action='store_true', help='Plot one year (x=1..12 months). Use --year YYYY to select; defaults to latest year in data')
    group.add_argument('--all', dest='plot_all', action='store_true', help='Plot all data aggregated by quarter over years')
    parser.add_argument('--month', help='YYYY-MM for monthly plot')
    parser.add_argument('--year', type=int, help='YYYY for yearly plot')
    parser.add_argument('--outdir', default=str(DEFAULT_OUTDIR), help='Output base directory (default: scripts/data-generation/out)')
    parser.add_argument('--datasets', default=str(DEFAULT_DATASETS_DIR), help='Datasets directory (default: out/datasets/demand)')
    args = parser.parse_args()

    outdir = Path(args.outdir)
    datasets = Path(args.datasets)
    _ensure_dirs(outdir)

    df, product_label = _load_csv(str(args.product), datasets)

    if args.monthly:
        plot_monthly(df, product_label, outdir, args.month)
    elif args.yearly:
        plot_yearly(df, product_label, outdir, args.year)
    else:
        plot_all_quarters(df, product_label, outdir)


if __name__ == '__main__':
    main()
