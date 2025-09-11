import os
from pathlib import Path


class Settings:
    INVENTORY_BASE_URL: str = os.getenv("INVENTORY_BASE_URL", "http://localhost:8000")
    MODEL_DIR: Path = Path(os.getenv("MODEL_DIR", "services/forecast/models")).resolve()
    FORECAST_DB_DSN: str | None = os.getenv("FORECAST_DB_DSN")
    CELERY_BROKER_URL: str = os.getenv("CELERY_BROKER_URL", "redis://localhost:6379/0")


settings = Settings()
settings.MODEL_DIR.mkdir(parents=True, exist_ok=True)

