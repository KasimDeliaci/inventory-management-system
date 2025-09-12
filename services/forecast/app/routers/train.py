from __future__ import annotations

import uuid
from fastapi import APIRouter, BackgroundTasks

from ..schemas import TrainRequest, TrainResponse
from ..workers.background import run_training_job
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
