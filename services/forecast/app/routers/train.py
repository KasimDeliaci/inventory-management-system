from __future__ import annotations

import uuid
from fastapi import APIRouter

from ..schemas import TrainRequest, TrainResponse


router = APIRouter()


@router.post("", response_model=TrainResponse, status_code=202)
def train(req: TrainRequest) -> TrainResponse:
    # PoC: stub a task id; Celery wiring can be added next
    task_id = str(uuid.uuid4())
    return TrainResponse(taskId=task_id, status="queued", statusUrl=f"/train/status/{task_id}")


@router.get("/status/{task_id}")
def train_status(task_id: str):
    # PoC: always return queued; wire to Celery result backend later
    return {"task_id": task_id, "status": "queued"}

