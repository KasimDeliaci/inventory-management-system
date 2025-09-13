from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np
import pandas as pd

try:
    import xgboost as xgb
except Exception:  # pragma: no cover
    xgb = None  # type: ignore

from ..clients.inventory import InventoryClient
from ..config import settings
from ..features.calendar import build_calendar
from ..features.assembler import add_lags_ma
from ..features.windows import build_date_indexed, densify_spine, aggregate_window


@dataclass
class HeadArtifact:
    model_path: str
    feature_names: List[str]
    q10: float
    q90: float


@dataclass
class XGBThreeArtifact:
    version_label: str
    model_name: str
    algorithm: str
    training_window_days: int
    h1: HeadArtifact
    y7: HeadArtifact
    y14: HeadArtifact


def _df_from_rows(rows: List[dict]) -> pd.DataFrame:
    df = pd.DataFrame(rows or [])
    if not df.empty and "date" in df.columns:
        df["date"] = pd.to_datetime(df["date"]).dt.normalize()
    return df


def _prep_series_per_product(
    product_id: int,
    spine: pd.DataFrame,
    sales_df: pd.DataFrame,
    promo_df: pd.DataFrame,
    offer_df: pd.DataFrame,
    cal_df: pd.DataFrame,
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    s = sales_df[sales_df["productId"] == product_id][["date", "salesUnits", "offerActiveShare"]].copy() if not sales_df.empty else pd.DataFrame(columns=["date","salesUnits","offerActiveShare"])
    p = promo_df[promo_df["productId"] == product_id][["date", "promoPct"]].copy() if not promo_df.empty else pd.DataFrame(columns=["date","promoPct"])
    s = s.set_index("date").sort_index()
    p = p.set_index("date").sort_index()
    o = offer_df.set_index("date").sort_index()
    c = cal_df.set_index("date").sort_index()

    sdf = spine.join(s, how="left").fillna({"salesUnits": 0.0, "offerActiveShare": 0.0})
    pdf = spine.join(p, how="left").fillna({"promoPct": 0.0})
    odf = spine.join(o, how="left").fillna({"offerAvgPct": 0.0, "offerMaxPct": 0.0, "activeOffersCount": 0})
    cdf = spine.join(c, how="left").fillna(False)
    return sdf, pdf, odf, cdf


def _make_examples_for_product(
    product_id: int,
    sdf: pd.DataFrame,
    pdf: pd.DataFrame,
    odf: pd.DataFrame,
    cdf: pd.DataFrame,
    max_h: int = 14,
) -> Tuple[pd.DataFrame, pd.Series, pd.DataFrame, pd.Series, pd.DataFrame, pd.Series]:
    # Add lags/MA
    ddf = sdf.reset_index().rename(columns={"index": "date"})
    ddf = add_lags_ma(ddf, ["salesUnits"]).sort_values("date")
    ddf["date"] = pd.to_datetime(ddf["date"]).dt.normalize()
    ddf = ddf.set_index("date").sort_index()

    # Build examples for each t where full horizon fits
    dates = ddf.index
    # We'll compute y at t+1, sum7, sum14
    X1_rows: List[Dict] = []
    y1_vals: List[float] = []
    X7_rows: List[Dict] = []
    y7_vals: List[float] = []
    X14_rows: List[Dict] = []
    y14_vals: List[float] = []

    # Helper to get window target sum safely
    def sum_future(start_excl_ts: pd.Timestamp, horizon: int) -> float:
        start = start_excl_ts + pd.Timedelta(days=1)
        end = start + pd.Timedelta(days=horizon - 1)
        s = ddf.loc[start:end, "salesUnits"] if (start in ddf.index or end in ddf.index) else pd.Series(dtype=float)
        return float(pd.to_numeric(s, errors="coerce").fillna(0.0).sum()) if len(s) else 0.0

    for t in dates:
        # ensure we have full horizon within index
        if (t + pd.Timedelta(days=1)) not in ddf.index:
            continue
        if (t + pd.Timedelta(days=7)) not in ddf.index:
            continue
        if (t + pd.Timedelta(days=max_h)) not in ddf.index:
            continue

        row_base: Dict[str, float] = {
            "productId": int(product_id),
        }
        # Historical lags/MAs at t
        for col in ["salesUnits_lag7", "salesUnits_lag14", "salesUnits_lag28", "salesUnits_ma7", "salesUnits_ma14", "salesUnits_ma28"]:
            val = float(ddf.loc[t, col]) if col in ddf.columns and pd.notna(ddf.loc[t, col]) else 0.0
            row_base[col] = val
        for col in ["offerActiveShare"]:
            # past realized at t
            val = float(ddf.loc[t, col]) if col in ddf.columns and pd.notna(ddf.loc[t, col]) else 0.0
            row_base[f"{col}_t"] = val

        # Exogenous for t+1 (point) for h1
        tp1 = t + pd.Timedelta(days=1)
        promo_d1 = float(pdf.loc[tp1, "promoPct"]) if tp1 in pdf.index else 0.0
        offer_avg_d1 = float(odf.loc[tp1, "offerAvgPct"]) if tp1 in odf.index else 0.0
        offer_max_d1 = float(odf.loc[tp1, "offerMaxPct"]) if tp1 in odf.index else 0.0
        active_cnt_d1 = float(odf.loc[tp1, "activeOffersCount"]) if tp1 in odf.index else 0.0

        X1 = dict(row_base)
        X1.update({
            "promoPct_d1": promo_d1,
            "offerAvgPct_d1": offer_avg_d1,
            "offerMaxPct_d1": offer_max_d1,
            "activeOffersCount_d1": active_cnt_d1,
        })
        y1 = float(ddf.loc[tp1, "salesUnits"]) if tp1 in ddf.index else 0.0

        # Window aggregates for y7/y14
        w7 = aggregate_window(t.date(), 7, pdf, odf, cdf)
        w14 = aggregate_window(t.date(), 14, pdf, odf, cdf)

        X7 = dict(row_base)
        X7.update(w7)
        y7 = sum_future(t, 7)

        X14 = dict(row_base)
        X14.update(w14)
        y14 = sum_future(t, 14)

        X1_rows.append(X1)
        y1_vals.append(y1)
        X7_rows.append(X7)
        y7_vals.append(y7)
        X14_rows.append(X14)
        y14_vals.append(y14)

    X1_df = pd.DataFrame(X1_rows)
    y1_s = pd.Series(y1_vals, name="y1")
    X7_df = pd.DataFrame(X7_rows)
    y7_s = pd.Series(y7_vals, name="y7")
    X14_df = pd.DataFrame(X14_rows)
    y14_s = pd.Series(y14_vals, name="y14")
    return X1_df, y1_s, X7_df, y7_s, X14_df, y14_s


def _time_split(df: pd.DataFrame, y: pd.Series, time_col: Optional[str] = None, val_frac: float = 0.15, test_frac: float = 0.15):
    n = len(df)
    if n == 0:
        return (df, y, df, y, df, y)
    i1 = max(1, int(round(n * (1.0 - (val_frac + test_frac)))))
    i2 = max(i1 + 1, int(round(n * (1.0 - test_frac))))
    X_tr, y_tr = df.iloc[:i1], y.iloc[:i1]
    X_val, y_val = df.iloc[i1:i2], y.iloc[i1:i2]
    X_te, y_te = df.iloc[i2:], y.iloc[i2:]
    return X_tr, y_tr, X_val, y_val, X_te, y_te


def _normalize_overrides(overrides: Optional[Dict]) -> Dict:
    """Normalize user-provided hyperparams to xgboost param names and supported keys.

    Supported aliases:
      - learning_rate -> eta
      - n_estimators / num_boost_round (returned separately)
    """
    o = dict(overrides or {})
    if "learning_rate" in o and "eta" not in o:
        o["eta"] = o.pop("learning_rate")
    return o


def _build_xgb_params(seed: int, overrides: Optional[Dict]) -> Tuple[Dict, int, int, Dict]:
    """Return (params, num_boost_round, early_stopping_rounds, used_hparams)"""
    o = _normalize_overrides(overrides)
    # Defaults
    params = {
        "objective": "reg:squarederror",
        "eval_metric": "rmse",
        "max_depth": 7,
        "eta": 0.08,
        "subsample": 0.8,
        "colsample_bytree": 0.8,
        "min_child_weight": 3,
        "seed": seed,
    }
    # Optional regularization
    if "reg_lambda" in o:
        params["reg_lambda"] = o["reg_lambda"]
    if "reg_alpha" in o:
        params["reg_alpha"] = o["reg_alpha"]
    # Merge known overrides
    for k in ("max_depth", "eta", "subsample", "colsample_bytree", "min_child_weight"):
        if k in o:
            params[k] = o[k]
    # Booster rounds & ES
    num_boost_round = int(o.get("n_estimators", o.get("num_boost_round", 1200)))
    early_stopping_rounds = int(o.get("early_stopping_rounds", 50))

    used = {
        "max_depth": params["max_depth"],
        "eta": params["eta"],
        "subsample": params["subsample"],
        "colsample_bytree": params["colsample_bytree"],
        "min_child_weight": params["min_child_weight"],
        "reg_lambda": params.get("reg_lambda", None),
        "reg_alpha": params.get("reg_alpha", None),
        "n_estimators": num_boost_round,
        "early_stopping_rounds": early_stopping_rounds,
    }
    return params, num_boost_round, early_stopping_rounds, used


def _train_head(
    X: pd.DataFrame,
    y: pd.Series,
    seed: int = 42,
    overrides: Optional[Dict] = None,
) -> Tuple[Optional[object], Dict[str, float], Tuple[float, float], Dict]:
    if xgb is None:
        return None, {}, (0.0, 0.0), {}
    # Fill NaNs
    X = X.fillna(0.0)
    # Split timewise
    X_tr, y_tr, X_val, y_val, X_te, y_te = _time_split(X, y)
    dtrain = xgb.DMatrix(X_tr, label=y_tr)
    dval = xgb.DMatrix(X_val, label=y_val)
    dtest = xgb.DMatrix(X_te, label=y_te)

    params, num_boost_round, early_stopping_rounds, used = _build_xgb_params(seed, overrides)
    evallist = [(dtrain, "train"), (dval, "eval")]
    bst = xgb.train(params, dtrain, num_boost_round=num_boost_round, evals=evallist, early_stopping_rounds=early_stopping_rounds, verbose_eval=False)

    # Metrics
    yhat_te = bst.predict(dtest)
    y_true = y_te.to_numpy(dtype=float)
    rmse = float(np.sqrt(np.mean((yhat_te - y_true) ** 2))) if len(y_true) else 0.0
    mae = float(np.mean(np.abs(yhat_te - y_true))) if len(y_true) else 0.0
    mape = float(np.mean(np.abs((yhat_te - y_true) / np.maximum(1e-6, y_true)))) * 100.0 if len(y_true) else 0.0

    # Conformal quantiles from validation residuals
    yhat_val = bst.predict(dval)
    yv = y_val.to_numpy(dtype=float)
    res = (yv - yhat_val) if len(yv) else np.array([0.0])
    q10 = float(np.quantile(res, 0.10))
    q90 = float(np.quantile(res, 0.90))

    return bst, {"rmse": rmse, "mae": mae, "mape": mape}, (q10, q90), used


def train_xgb_three(task_id: str, params: Dict) -> Tuple[XGBThreeArtifact, Dict[str, Dict[str, float]], Dict]:
    """Train three heads: h1 daily, y7 sum, y14 sum. Returns artifact spec and metrics per head."""
    rng_seed = int(params.get("seed", 42))
    window_days = int(params.get("trainWindowDays", 1095))  # ~36 months
    as_of = params.get("asOfDate")
    if as_of:
        as_of = pd.to_datetime(as_of).date()
    else:
        as_of = date.today()

    start = as_of - timedelta(days=window_days + 60)  # headroom for lags and horizons
    end = as_of

    # Fetch data
    inv = InventoryClient()
    sales_rows = inv.get_product_day_sales(start, end, None)
    promo_rows = inv.get_product_day_promo(start, end, None)
    offer_rows = inv.get_day_offer_stats(start, end)

    sales_df = _df_from_rows(sales_rows)
    promo_df = _df_from_rows(promo_rows)
    offer_df = _df_from_rows(offer_rows)

    # Determine product ids present
    product_ids = sorted(set([int(x) for x in (sales_df["productId"].unique().tolist() if "productId" in sales_df.columns else [])]))
    # If none via sales, fallback to promo set
    if not product_ids and "productId" in promo_df.columns and not promo_df.empty:
        product_ids = sorted(set(int(x) for x in promo_df["productId"].unique().tolist()))

    # Calendar
    cal = build_calendar(start, end)
    cal["date"] = pd.to_datetime(cal["date"]).dt.normalize()
    cal_df = cal

    # Dense spine
    spine = densify_spine(start, end)

    # Collect examples
    X1_list: List[pd.DataFrame] = []
    y1_list: List[pd.Series] = []
    X7_list: List[pd.DataFrame] = []
    y7_list: List[pd.Series] = []
    X14_list: List[pd.DataFrame] = []
    y14_list: List[pd.Series] = []

    for pid in product_ids:
        sdf, pdf, odf, cdf = _prep_series_per_product(pid, spine, sales_df, promo_df, offer_df, cal_df)
        X1_df, y1_s, X7_df, y7_s, X14_df, y14_s = _make_examples_for_product(pid, sdf, pdf, odf, cdf)
        if not X1_df.empty:
            X1_list.append(X1_df)
            y1_list.append(y1_s)
        if not X7_df.empty:
            X7_list.append(X7_df)
            y7_list.append(y7_s)
        if not X14_df.empty:
            X14_list.append(X14_df)
            y14_list.append(y14_s)

    if not X1_list:
        raise RuntimeError("No training data assembled for h1")

    X1 = pd.concat(X1_list, ignore_index=True)
    y1 = pd.concat(y1_list, ignore_index=True)
    X7 = pd.concat(X7_list, ignore_index=True) if X7_list else pd.DataFrame()
    y7 = pd.concat(y7_list, ignore_index=True) if y7_list else pd.Series(dtype=float)
    X14 = pd.concat(X14_list, ignore_index=True) if X14_list else pd.DataFrame()
    y14 = pd.concat(y14_list, ignore_index=True) if y14_list else pd.Series(dtype=float)

    overrides = params.get("hyperparams", {})
    # Train heads (apply same overrides to each head)
    bst1, m1, (q10_1, q90_1), used_hp = _train_head(
        X1.drop(columns=["productId"], errors="ignore"), y1, seed=rng_seed, overrides=overrides
    )
    if not X7.empty:
        bst7, m7, (q10_7, q90_7), _ = _train_head(
            X7.drop(columns=["productId"], errors="ignore"), y7, seed=rng_seed, overrides=overrides
        )
    else:
        bst7, m7, (q10_7, q90_7) = (None, {}, (0.0, 0.0))
    if not X14.empty:
        bst14, m14, (q10_14, q90_14), _ = _train_head(
            X14.drop(columns=["productId"], errors="ignore"), y14, seed=rng_seed, overrides=overrides
        )
    else:
        bst14, m14, (q10_14, q90_14) = (None, {}, (0.0, 0.0))

    # Persist artifacts
    version_label = f"xgb_three-{pd.Timestamp.utcnow().strftime('%Y%m%d%H%M%S')}"
    root = Path(settings.MODEL_DIR) / "xgb_three" / version_label
    (root / "h1").mkdir(parents=True, exist_ok=True)
    (root / "y7").mkdir(parents=True, exist_ok=True)
    (root / "y14").mkdir(parents=True, exist_ok=True)

    f1 = list(X1.drop(columns=["productId"], errors="ignore").columns)
    f7 = list(X7.drop(columns=["productId"], errors="ignore").columns) if not X7.empty else []
    f14 = list(X14.drop(columns=["productId"], errors="ignore").columns) if not X14.empty else []

    art1_path = str(root / "h1" / "model.json")
    art7_path = str(root / "y7" / "model.json")
    art14_path = str(root / "y14" / "model.json")

    if bst1 is not None:
        bst1.save_model(art1_path)
    if bst7 is not None:
        bst7.save_model(art7_path)
    if bst14 is not None:
        bst14.save_model(art14_path)

    # Save metadata
    meta = {
        "version_label": version_label,
        "model_name": "xgb_three",
        "algorithm": "xgb_three",
        "training_window_days": window_days,
        "heads": {
            "h1": {"features": f1, "q10": q10_1, "q90": q90_1},
            "y7": {"features": f7, "q10": q10_7, "q90": q90_7},
            "y14": {"features": f14, "q10": q10_14, "q90": q90_14},
        },
    }
    (root / "meta.json").write_text(json.dumps(meta, indent=2))

    artifact = XGBThreeArtifact(
        version_label=version_label,
        model_name="xgb_three",
        algorithm="xgb_three",
        training_window_days=window_days,
        h1=HeadArtifact(model_path=art1_path, feature_names=f1, q10=q10_1, q90=q90_1),
        y7=HeadArtifact(model_path=art7_path, feature_names=f7, q10=q10_7, q90=q90_7),
        y14=HeadArtifact(model_path=art14_path, feature_names=f14, q10=q10_14, q90=q90_14),
    )

    metrics = {"h1": m1, "y7": m7, "y14": m14}
    return artifact, metrics, used_hp


def tune_xgb_three(params: Dict) -> Dict:
    """One-off tuning: random search over hyperparams without saving artifacts.

    Returns dict with bestHyperparams, bestMetrics, and trials summary.
    """
    rng_seed = int(params.get("seed", 42))
    window_days = int(params.get("trainWindowDays", 1095))
    trials = int(params.get("tuneTrials", 20))
    as_of = params.get("asOfDate")
    if as_of:
        as_of = pd.to_datetime(as_of).date()
    else:
        as_of = date.today()

    start = as_of - timedelta(days=window_days + 60)
    end = as_of

    inv = InventoryClient()
    sales_rows = inv.get_product_day_sales(start, end, None)
    promo_rows = inv.get_product_day_promo(start, end, None)
    offer_rows = inv.get_day_offer_stats(start, end)

    sales_df = _df_from_rows(sales_rows)
    promo_df = _df_from_rows(promo_rows)
    offer_df = _df_from_rows(offer_rows)
    product_ids = sorted(set([int(x) for x in (sales_df["productId"].unique().tolist() if "productId" in sales_df.columns else [])]))
    if not product_ids and "productId" in promo_df.columns and not promo_df.empty:
        product_ids = sorted(set(int(x) for x in promo_df["productId"].unique().tolist()))

    cal = build_calendar(start, end)
    cal["date"] = pd.to_datetime(cal["date"]).dt.normalize()
    spine = densify_spine(start, end)

    X1_list: List[pd.DataFrame] = []
    y1_list: List[pd.Series] = []
    X7_list: List[pd.DataFrame] = []
    y7_list: List[pd.Series] = []
    X14_list: List[pd.DataFrame] = []
    y14_list: List[pd.Series] = []
    for pid in product_ids:
        sdf, pdf, odf, cdf = _prep_series_per_product(pid, spine, sales_df, promo_df, offer_df, cal)
        X1_df, y1_s, X7_df, y7_s, X14_df, y14_s = _make_examples_for_product(pid, sdf, pdf, odf, cdf)
        if not X1_df.empty:
            X1_list.append(X1_df)
            y1_list.append(y1_s)
        if not X7_df.empty:
            X7_list.append(X7_df)
            y7_list.append(y7_s)
        if not X14_df.empty:
            X14_list.append(X14_df)
            y14_list.append(y14_s)

    if not X1_list:
        raise RuntimeError("No training data assembled for tuning")

    X1 = pd.concat(X1_list, ignore_index=True)
    y1 = pd.concat(y1_list, ignore_index=True)
    X7 = pd.concat(X7_list, ignore_index=True) if X7_list else pd.DataFrame()
    y7 = pd.concat(y7_list, ignore_index=True) if y7_list else pd.Series(dtype=float)
    X14 = pd.concat(X14_list, ignore_index=True) if X14_list else pd.DataFrame()
    y14 = pd.concat(y14_list, ignore_index=True) if y14_list else pd.Series(dtype=float)

    rng = np.random.default_rng(rng_seed)

    def sample_space() -> Dict:
        return {
            "max_depth": int(rng.choice([4, 6, 8, 10])),
            "eta": float(rng.choice([0.03, 0.05, 0.08, 0.1])),
            "subsample": float(rng.choice([0.7, 0.8, 1.0])),
            "colsample_bytree": float(rng.choice([0.7, 0.8, 1.0])),
            "min_child_weight": int(rng.choice([1, 3, 5])),
            "reg_lambda": float(rng.choice([0.0, 1.0, 5.0])),
            "reg_alpha": float(rng.choice([0.0, 0.1, 1.0])),
            "n_estimators": int(rng.choice([600, 800, 1000, 1200])),
            "early_stopping_rounds": 50,
        }

    best = None
    trials_out: List[Dict] = []
    for i in range(max(1, trials)):
        hp = sample_space()
        # Train heads quickly (no saving)
        bst1, m1, _, used1 = _train_head(X1.drop(columns=["productId"], errors="ignore"), y1, seed=rng_seed + i, overrides=hp)
        if not X7.empty:
            _, m7, _, _ = _train_head(X7.drop(columns=["productId"], errors="ignore"), y7, seed=rng_seed + i, overrides=hp)
        else:
            m7 = {"rmse": float("inf")}
        if not X14.empty:
            _, m14, _, _ = _train_head(X14.drop(columns=["productId"], errors="ignore"), y14, seed=rng_seed + i, overrides=hp)
        else:
            m14 = {"rmse": float("inf")}

        score = 0.6 * float(m7.get("rmse", 1e9)) + 0.3 * float(m14.get("rmse", 1e9)) + 0.1 * float(m1.get("rmse", 1e9))
        rec = {"trial": i + 1, "hyperparams": hp, "metrics": {"h1": m1, "y7": m7, "y14": m14}, "score": score}
        trials_out.append(rec)
        if best is None or score < best[0]:
            best = (score, hp, {"h1": m1, "y7": m7, "y14": m14})

    result = {
        "bestHyperparams": best[1] if best else {},
        "bestMetrics": best[2] if best else {},
        "trials": trials_out,
        "asOfDate": as_of.isoformat(),
        "trainWindowDays": window_days,
    }
    return result


def load_artifact(artifact_path: str) -> Optional[XGBThreeArtifact]:
    try:
        meta_p = Path(artifact_path) / "meta.json"
        meta = json.loads(meta_p.read_text())
        heads = meta.get("heads", {})
        root = Path(artifact_path)
        return XGBThreeArtifact(
            version_label=meta.get("version_label", "unknown"),
            model_name=meta.get("model_name", "xgb_three"),
            algorithm=meta.get("algorithm", "xgb_three"),
            training_window_days=int(meta.get("training_window_days", 365)),
            h1=HeadArtifact(str(root / "h1" / "model.json"), heads.get("h1", {}).get("features", []), float(heads.get("h1", {}).get("q10", 0.0)), float(heads.get("h1", {}).get("q90", 0.0))),
            y7=HeadArtifact(str(root / "y7" / "model.json"), heads.get("y7", {}).get("features", []), float(heads.get("y7", {}).get("q10", 0.0)), float(heads.get("y7", {}).get("q90", 0.0))),
            y14=HeadArtifact(str(root / "y14" / "model.json"), heads.get("y14", {}).get("features", []), float(heads.get("y14", {}).get("q10", 0.0)), float(heads.get("y14", {}).get("q90", 0.0))),
        )
    except Exception:
        return None


def _predict_head(model_path: str, feature_names: List[str], X: pd.DataFrame) -> np.ndarray:
    if xgb is None:
        return np.zeros(len(X))
    bst = xgb.Booster()
    bst.load_model(model_path)
    d = xgb.DMatrix(X.fillna(0.0)[feature_names])
    return bst.predict(d)


def infer_xgb_three(
    artifact: XGBThreeArtifact,
    product_ids: List[int],
    as_of: date,
    horizon_days: int,
) -> Dict[int, Dict[str, object]]:
    """Predict for given products and horizon using saved artifacts.

    Returns dict per productId with keys:
      - daily (List[float])
      - sum (float)
      - lower (float)
      - upper (float)
    """
    inv = InventoryClient()
    # History window for lags/MA
    start_hist = as_of - timedelta(days=max(365, artifact.training_window_days // 3))
    end_hist = as_of
    # Fetch history and future exogenous
    sales_rows = inv.get_product_day_sales(start_hist, end_hist, product_ids)
    promo_rows_hist = inv.get_product_day_promo(start_hist, end_hist, product_ids)
    promo_rows_future = inv.get_product_day_promo(as_of + timedelta(days=1), as_of + timedelta(days=horizon_days), product_ids)
    offer_rows_hist = inv.get_day_offer_stats(start_hist, end_hist)
    offer_rows_future = inv.get_day_offer_stats(as_of + timedelta(days=1), as_of + timedelta(days=horizon_days))

    sales_df = _df_from_rows(sales_rows)
    promo_hist_df = _df_from_rows(promo_rows_hist)
    promo_future_df = _df_from_rows(promo_rows_future)
    offer_hist_df = _df_from_rows(offer_rows_hist)
    offer_future_df = _df_from_rows(offer_rows_future)

    # Calendar across history+horizon
    cal = build_calendar(start_hist, as_of + timedelta(days=horizon_days))
    cal["date"] = pd.to_datetime(cal["date"]).dt.normalize()

    # Prepare outputs
    results: Dict[int, Dict[str, object]] = {}

    # Build per-product feature row at t=as_of
    for pid in product_ids:
        # Build series
        # History spine up to as_of for lags
        hist_spine = densify_spine(start_hist, as_of)
        sdf, pdf_hist, odf_hist, cdf_hist = _prep_series_per_product(pid, hist_spine, sales_df, promo_hist_df, offer_hist_df, cal)
        # Future frames for window aggregates and weights
        fut_spine = densify_spine(as_of + timedelta(days=1), as_of + timedelta(days=horizon_days))
        # Promo future per product
        pf = promo_future_df[promo_future_df["productId"] == pid][["date", "promoPct"]].copy() if not promo_future_df.empty else pd.DataFrame(columns=["date","promoPct"])
        pf = pf.set_index(pd.to_datetime(pf["date"]).dt.normalize() if not pf.empty else pd.Index([])).sort_index()
        pf.index.name = None
        pf = fut_spine.join(pf.drop(columns=["date"], errors="ignore"), how="left").fillna({"promoPct": 0.0})
        of = _df_from_rows(offer_rows_future).set_index("date") if not offer_future_df.empty else pd.DataFrame()
        if not of.empty:
            of = fut_spine.join(of, how="left").fillna({"offerAvgPct": 0.0, "offerMaxPct": 0.0, "activeOffersCount": 0})
        else:
            of = fut_spine.copy()
            of["offerAvgPct"] = 0.0
            of["offerMaxPct"] = 0.0
            of["activeOffersCount"] = 0
        cf = cal.copy()
        cf["date"] = pd.to_datetime(cf["date"]).dt.normalize()
        cf = cf.set_index("date").sort_index()
        cf = fut_spine.join(cf, how="left").fillna(False)

        # Lags/MAs up to as_of
        hist_df = sdf.reset_index().rename(columns={"index": "date"})
        hist_df = add_lags_ma(hist_df, ["salesUnits"]).sort_values("date")
        hist_df["date"] = pd.to_datetime(hist_df["date"]).dt.normalize()
        hist_df = hist_df.set_index("date").sort_index()
        t = pd.Timestamp(as_of)
        row_base: Dict[str, float] = {
            "productId": int(pid),
        }
        for col in ["salesUnits_lag7", "salesUnits_lag14", "salesUnits_lag28", "salesUnits_ma7", "salesUnits_ma14", "salesUnits_ma28"]:
            val = float(hist_df.loc[t, col]) if col in hist_df.columns and t in hist_df.index and pd.notna(hist_df.loc[t, col]) else 0.0
            row_base[col] = val
        val = float(hist_df.loc[t, "offerActiveShare"]) if "offerActiveShare" in hist_df.columns and t in hist_df.index and pd.notna(hist_df.loc[t, "offerActiveShare"]) else 0.0
        row_base["offerActiveShare_t"] = val

        # Predict
        if horizon_days == 1:
            # Point exogenous for d1
            d1 = pd.Timestamp(as_of + timedelta(days=1))
            X = dict(row_base)
            X.update({
                "promoPct_d1": float(pf.loc[d1, "promoPct"]) if d1 in pf.index else 0.0,
                "offerAvgPct_d1": float(of.loc[d1, "offerAvgPct"]) if d1 in of.index else 0.0,
                "offerMaxPct_d1": float(of.loc[d1, "offerMaxPct"]) if d1 in of.index else 0.0,
                "activeOffersCount_d1": float(of.loc[d1, "activeOffersCount"]) if d1 in of.index else 0.0,
            })
            Xdf = pd.DataFrame([X])
            yhat = _predict_head(artifact.h1.model_path, artifact.h1.feature_names, Xdf)
            y = float(max(0.0, yhat[0]))
            lower = max(0.0, y + artifact.h1.q10)
            upper = max(lower, y + artifact.h1.q90)
            results[pid] = {"daily": [y], "sum": y, "lower": lower, "upper": upper}
        else:
            # Window aggregates for y7/y14
            w = aggregate_window(as_of, horizon_days, pf, of, cf)
            X = dict(row_base)
            X.update(w)
            Xdf = pd.DataFrame([X])
            if horizon_days == 7:
                yhat = _predict_head(artifact.y7.model_path, artifact.y7.feature_names, Xdf)
                ysum = float(max(0.0, yhat[0]))
                lower = max(0.0, ysum + artifact.y7.q10)
                upper = max(lower, ysum + artifact.y7.q90)
            else:
                yhat = _predict_head(artifact.y14.model_path, artifact.y14.feature_names, Xdf)
                ysum = float(max(0.0, yhat[0]))
                lower = max(0.0, ysum + artifact.y14.q10)
                upper = max(lower, ysum + artifact.y14.q90)

            # Allocate daily weights
            # Reuse promo/offer/calendar frames built above
            from ..features.windows import daily_weights
            weights = daily_weights(as_of, horizon_days, pf, of, cf)
            daily_vals = [max(0.0, float(ysum * w)) for w in weights]
            # Adjust rounding drift
            drift = ysum - float(sum(daily_vals))
            if abs(drift) > 1e-6 and len(daily_vals) > 0:
                daily_vals[-1] += float(drift)
                if daily_vals[-1] < 0:
                    daily_vals[-1] = 0.0
            results[pid] = {"daily": daily_vals, "sum": ysum, "lower": lower, "upper": upper}

    return results
