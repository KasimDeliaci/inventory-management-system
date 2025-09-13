from fastapi import FastAPI
from fastapi.responses import RedirectResponse, Response

from .routers.train import router as train_router
from .routers.forecast import router as forecast_router


app = FastAPI(title="Forecast Service")

app.include_router(train_router, prefix="/train", tags=["Train"])
app.include_router(forecast_router, prefix="/forecast", tags=["Forecast"])


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/", include_in_schema=False)
def root():
    # Friendly landing: redirect to interactive API docs
    return RedirectResponse(url="/docs")


@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    # Avoid noisy 404s when browsers request a favicon
    return Response(status_code=204)
