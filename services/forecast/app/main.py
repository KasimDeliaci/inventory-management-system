from fastapi import FastAPI

from .routers.train import router as train_router
from .routers.forecast import router as forecast_router


app = FastAPI(title="Forecast Service")

app.include_router(train_router, prefix="/train", tags=["Train"])
app.include_router(forecast_router, prefix="/forecast", tags=["Forecast"])


@app.get("/health")
def health():
    return {"status": "ok"}

