from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any, Dict, Optional

from ..config import settings
from ..db import dal


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

        # Simulate work (replace with real training pipeline later)
        time.sleep(0.5)

        model_name = "baseline"
        version_label = "baseline-0.1"
        algorithm = params.get("algorithm", "baseline_ma7")
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

        if rid:
            dal.update_training_run(rid, "succeeded", metrics=backtest, model_version_id=mv_id)
    except Exception as e:  # pragma: no cover - background failure path
        if rid:
            dal.update_training_run(rid, "failed", error_message=str(e))

