from __future__ import annotations

import os
from contextlib import contextmanager
from typing import Any, Dict, Iterable, Optional


_PSYCOPG_AVAILABLE = True
try:
    import psycopg
    from psycopg.types.json import Json  # psycopg3 JSON adapter
except Exception:  # pragma: no cover - optional dependency
    psycopg = None  # type: ignore
    def Json(v):  # type: ignore
        return v
    _PSYCOPG_AVAILABLE = False


def _dsn() -> Optional[str]:
    return os.getenv("FORECAST_DB_DSN")


@contextmanager
def get_conn():
    dsn = _dsn()
    if not dsn or not _PSYCOPG_AVAILABLE:
        yield None
        return
    with psycopg.connect(dsn) as conn:  # type: ignore[attr-defined]
        yield conn


def init_ok() -> bool:
    return bool(_dsn()) and _PSYCOPG_AVAILABLE


def insert_training_run(status: str, scope: str, product_id: Optional[int], params: Dict[str, Any]) -> Optional[int]:
    if not init_ok():
        return None
    sql = (
        "INSERT INTO training_runs (status, scope, product_id, params) VALUES (%s, %s, %s, %s) "
        "RETURNING training_run_id"
    )
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql, (status, scope, product_id, Json(params)))
            rid = cur.fetchone()[0]
            conn.commit()
            return int(rid)


def update_training_run(
    rid: int,
    status: str,
    metrics: Optional[Dict[str, Any]] = None,
    error_message: Optional[str] = None,
    model_version_id: Optional[int] = None,
) -> None:
    if not init_ok():
        return
    parts = ["status = %s"]
    vals: list[Any] = [status]
    if metrics is not None:
        parts.append("metrics = %s")
        vals.append(Json(metrics))
    if error_message is not None:
        parts.append("error_message = %s")
        vals.append(error_message)
    if model_version_id is not None:
        parts.append("model_version_id = %s")
        vals.append(model_version_id)
    # Timestamp handling:
    # - When moving to RUNNING, set started_at = NOW() (queue time may differ from actual start)
    # - Only set finished_at for terminal statuses (SUCCEEDED/FAILED)
    normalized = (status or "").strip().lower()
    if normalized == "running":
        parts.append("started_at = NOW()")
    if normalized in ("succeeded", "failed"):
        parts.append("finished_at = NOW()")
    sql = f"UPDATE training_runs SET {', '.join(parts)} WHERE training_run_id = %s"
    vals.append(rid)
    with get_conn() as conn:
        if conn is None:
            return
        with conn.cursor() as cur:
            cur.execute(sql, tuple(vals))
            conn.commit()


def insert_model_version(model_name: str, version_label: str, algorithm: str, artifact_path: str, training_window_days: int, hyperparams: Dict[str, Any], metrics: Dict[str, Any], is_active: bool = False) -> Optional[int]:
    if not init_ok():
        return None
    sql = (
        "INSERT INTO model_versions (model_name, version_label, algorithm, artifact_path, training_window_days, hyperparams, metrics, is_active) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s) RETURNING model_version_id"
    )
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql, (model_name, version_label, algorithm, artifact_path, training_window_days, Json(hyperparams), Json(metrics), is_active))
            mid = cur.fetchone()[0]
            conn.commit()
            return int(mid)


def get_active_model_version() -> Optional[dict]:
    if not init_ok():
        return None
    sql = "SELECT model_version_id, model_name, version_label, algorithm, artifact_path FROM model_versions WHERE is_active = TRUE ORDER BY trained_at DESC LIMIT 1"
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql)
            row = cur.fetchone()
            if not row:
                return None
            return {
                "model_version_id": int(row[0]),
                "model_name": row[1],
                "version_label": row[2],
                "algorithm": row[3],
                "artifact_path": row[4],
            }


def list_model_versions(limit: int = 50, offset: int = 0) -> Optional[list[dict]]:
    if not init_ok():
        return None
    sql = (
        "SELECT model_version_id, model_name, version_label, algorithm, trained_at, artifact_path, "
        "training_window_days, hyperparams, metrics, is_active "
        "FROM model_versions ORDER BY trained_at DESC LIMIT %s OFFSET %s"
    )
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql, (int(limit), int(offset)))
            rows = cur.fetchall() or []
            out: list[dict] = []
            for r in rows:
                out.append(
                    {
                        "model_version_id": int(r[0]),
                        "model_name": r[1],
                        "version_label": r[2],
                        "algorithm": r[3],
                        "trained_at": r[4],
                        "artifact_path": r[5],
                        "training_window_days": r[6],
                        "hyperparams": r[7],
                        "metrics": r[8],
                        "is_active": r[9],
                    }
                )
            return out


def get_model_version(model_version_id: int) -> Optional[dict]:
    if not init_ok():
        return None
    sql = (
        "SELECT model_version_id, model_name, version_label, algorithm, trained_at, artifact_path, training_window_days, hyperparams, metrics, is_active "
        "FROM model_versions WHERE model_version_id = %s"
    )
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql, (int(model_version_id),))
            r = cur.fetchone()
            if not r:
                return None
            return {
                "model_version_id": int(r[0]),
                "model_name": r[1],
                "version_label": r[2],
                "algorithm": r[3],
                "trained_at": r[4],
                "artifact_path": r[5],
                "training_window_days": r[6],
                "hyperparams": r[7],
                "metrics": r[8],
                "is_active": r[9],
            }


def set_active_model_version(model_version_id: int) -> bool:
    """Mark a model version active and deactivate others."""
    if not init_ok():
        return False
    with get_conn() as conn:
        if conn is None:
            return False
        with conn.cursor() as cur:
            cur.execute("UPDATE model_versions SET is_active = FALSE")
            cur.execute("UPDATE model_versions SET is_active = TRUE WHERE model_version_id = %s", (int(model_version_id),))
            conn.commit()
            return True


def insert_forecast_accuracy(product_id: int, forecast_date: str, horizon_days: int, predicted_value: float, actual_value: Optional[float], error_pct: Optional[float], model_version_id: Optional[int]) -> None:
    if not init_ok():
        return
    sql = (
        "INSERT INTO forecast_accuracy (product_id, forecast_date, horizon_days, predicted_value, actual_value, error_pct, model_version_id) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s)"
    )
    with get_conn() as conn:
        if conn is None:
            return
        with conn.cursor() as cur:
            cur.execute(sql, (int(product_id), forecast_date, int(horizon_days), float(predicted_value), actual_value, error_pct, model_version_id))
            conn.commit()


def insert_forecast(as_of_date: str, horizon_days: int, model_version_id: Optional[int], request_hash: Optional[str]) -> Optional[int]:
    if not init_ok():
        return None
    sql = (
        "INSERT INTO forecasts (as_of_date, horizon_days, model_version_id, request_hash) VALUES (%s, %s, %s, %s) "
        "RETURNING forecast_id"
    )
    with get_conn() as conn:
        if conn is None:
            return None
        with conn.cursor() as cur:
            cur.execute(sql, (as_of_date, int(horizon_days), model_version_id, request_hash))
            fid = cur.fetchone()[0]
            conn.commit()
            return int(fid)


def insert_forecast_items(forecast_id: int, items: Iterable[dict]) -> None:
    if not init_ok():
        return
    sql = (
        "INSERT INTO forecast_items (forecast_id, product_id, forecast_date, yhat, confidence, lower_bound, upper_bound) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s)"
    )
    with get_conn() as conn:
        if conn is None:
            return
        with conn.cursor() as cur:
            for it in items:
                confidence = it.get("confidence")
                cur.execute(
                    sql,
                    (
                        forecast_id,
                        int(it["productId"]),
                        it["date"],
                        float(it["yhat"]),
                        Json(confidence) if confidence is not None else Json({}),
                        it.get("lower"),
                        it.get("upper"),
                    ),
                )
            conn.commit()
