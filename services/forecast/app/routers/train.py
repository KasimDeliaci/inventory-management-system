from __future__ import annotations

import uuid
from fastapi import APIRouter, BackgroundTasks, HTTPException

from ..schemas import TrainRequest, TrainResponse
from ..workers.background import run_training_job
from ..models.xgb_three import tune_xgb_three, train_xgb_three
from ..db import dal
from ..db import dal


router = APIRouter()


@router.post("", response_model=TrainResponse, status_code=202)
def train(req: TrainRequest, bg: BackgroundTasks) -> TrainResponse:
    """Queue a background training task (in-process) and return a task id."""
    task_id = str(uuid.uuid4())
    bg.add_task(run_training_job, task_id, req.model_dump())
    return TrainResponse(taskId=task_id, status="queued", statusUrl=f"/train/status/{task_id}")


@router.get("/status/{task_id}")
def train_status(task_id: str):
    # PoC: without a task registry or DB, report queued; DAL-backed status can be added later
    return {"task_id": task_id, "status": "queued"}


@router.get("/models/active")
def get_active_model():
    """Return the currently active model version (if any)."""
    mv = dal.get_active_model_version()
    return mv or {}


@router.post("/activate/{model_version_id}")
def activate_model(model_version_id: int):
    """Activate a model version by id (sets others inactive)."""
    ok = dal.set_active_model_version(model_version_id)
    mv = dal.get_active_model_version() if ok else None
    return {"ok": ok, "active": mv}


@router.get("/models")
def list_models(limit: int = 50, offset: int = 0):
    """List model versions ordered by trained_at desc (DB optional)."""
    models = dal.list_model_versions(limit=limit, offset=offset)
    return models or []


@router.get("/models/{model_version_id}")
def get_model(model_version_id: int):
    """Get a single model version by id."""
    mv = dal.get_model_version(model_version_id)
    return mv or {}


@router.post("/tune")
def tune(
    trials: int = 20,
    trainWindowDays: int = 1095,
    seed: int = 42,
    asOfDate: str | None = None,
    trainBest: bool = True,
):
    """Run a one-off random search to suggest better hyperparams.

    - Returns the best hyperparams/metrics and (optionally) trains + activates the best model.
    - This endpoint runs synchronously; prefer moderate trial counts (e.g., 10–30).
    """
    params = {
        "tuneTrials": int(trials),
        "trainWindowDays": int(trainWindowDays),
        "seed": int(seed),
    }
    if asOfDate:
        params["asOfDate"] = asOfDate
    result = tune_xgb_three(params)

    model_info = {}
    if trainBest:
        # Train with best hyperparams and auto-activate
        hp = result.get("bestHyperparams", {})
        tparams = {
            "algorithm": "xgb_three",
            "trainWindowDays": int(trainWindowDays),
            "hyperparams": hp,
            "autoActivate": True,
        }
        # Use a direct call to training (synchronous) to avoid background queue for this one-off
        try:
            artifact, metrics, used = train_xgb_three("tune_inline", tparams)
            mv_metrics = {"h1": metrics.get("h1", {}), "y7": metrics.get("y7", {}), "y14": metrics.get("y14", {})}
            mv_id = dal.insert_model_version(
                model_name="xgb_three",
                version_label=artifact.version_label,
                algorithm="xgb_three",
                artifact_path=str(artifact.h1.model_path).replace("/h1/model.json", ""),
                training_window_days=int(trainWindowDays),
                hyperparams=used,
                metrics=mv_metrics,
                is_active=False,
            )
            if mv_id:
                dal.set_active_model_version(int(mv_id))
                model_info = {"model_version_id": int(mv_id), "version_label": artifact.version_label}
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to persist best model: {e}")

    out = {"best": result}
    if model_info:
        out["trainedAndActivated"] = model_info
    return out
