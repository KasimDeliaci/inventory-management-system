from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any, Dict, Optional

from ..config import settings
from ..db import dal
from ..models.xgb_three import train_xgb_three


def run_training_job(task_id: str, params: Dict[str, Any]) -> None:
    """Background training job (PoC).

    - Registers a training_run (QUEUED->RUNNING->SUCCEEDED/FAILED)
    - Writes a tiny artifact file under MODEL_DIR
    - Registers a model_versions row with placeholder metrics
    """
    scope = params.get("scope", "global") or "global"
    product_id = params.get("productId")

    rid = dal.insert_training_run("queued", scope, product_id, params) or 0
    try:
        # Mark RUNNING
        if rid:
            dal.update_training_run(rid, "running")

        algorithm = str(params.get("algorithm", "baseline_ma7"))
        if algorithm == "xgb_three":
            # Train three XGB heads and persist artifacts/metrics
            artifact, metrics, used_hparams = train_xgb_three(task_id, params)
            mv_metrics = {
                "h1": metrics.get("h1", {}),
                "y7": metrics.get("y7", {}),
                "y14": metrics.get("y14", {}),
            }
            mv_id = dal.insert_model_version(
                model_name="xgb_three",
                version_label=artifact.version_label,
                algorithm="xgb_three",
                artifact_path=str((Path(settings.MODEL_DIR) / "xgb_three" / artifact.version_label).resolve()),
                training_window_days=int(params.get("trainWindowDays", artifact.training_window_days)),
                hyperparams=used_hparams or params.get("hyperparams", {}),
                metrics=mv_metrics,
                is_active=False,
            ) or None
            # Auto-activate the newly trained model
            auto_activate = bool(params.get("autoActivate", True))
            if mv_id and auto_activate:
                try:
                    dal.set_active_model_version(mv_id)
                except Exception:
                    pass
        else:
            # Baseline stub (existing behavior)
            time.sleep(0.5)
            model_name = "baseline"
            version_label = "baseline-0.1"
            train_window = int(params.get("trainWindowDays", 365))
            artifact_dir = Path(settings.MODEL_DIR) / model_name / version_label
            artifact_dir.mkdir(parents=True, exist_ok=True)
            artifact_path = artifact_dir / "model.json"
            artifact = {
                "taskId": task_id,
                "algorithm": algorithm,
                "hyperparams": params.get("hyperparams", {}),
            }
            artifact_path.write_text(json.dumps(artifact, indent=2))
            backtest = {"backtest_metrics": {"1_day_mape": 0.20, "7_day_mape": 0.25, "14_day_mape": 0.30}}
            mv_id = dal.insert_model_version(
                model_name=model_name,
                version_label=version_label,
                algorithm=algorithm,
                artifact_path=str(artifact_path),
                training_window_days=train_window,
                hyperparams=params.get("hyperparams", {}),
                metrics=backtest,
                is_active=False,
            ) or None
            auto_activate = bool(params.get("autoActivate", True))
            if mv_id and auto_activate:
                try:
                    dal.set_active_model_version(mv_id)
                except Exception:
                    pass

        if rid:
            # For xgb_three, mv_metrics; for baseline, backtest
            metrics_payload = mv_metrics if algorithm == "xgb_three" else backtest
            dal.update_training_run(rid, "succeeded", metrics=metrics_payload, model_version_id=mv_id)
    except Exception as e:  # pragma: no cover - background failure path
        if rid:
            dal.update_training_run(rid, "failed", error_message=str(e))
