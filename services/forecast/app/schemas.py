from datetime import date
from typing import List, Optional
from pydantic import BaseModel, Field


class TrainRequest(BaseModel):
    scope: str = Field(default="global", description="'global' or 'product'")
    productId: Optional[int] = None
    trainWindowDays: int = 365
    algorithm: str = "baseline"
    hyperparams: dict = Field(default_factory=dict)


class TrainResponse(BaseModel):
    taskId: str
    status: str = "queued"
    statusUrl: str


class ForecastRequest(BaseModel):
    productIds: List[int]
    horizonDays: int = Field(default=7, description="1 | 7 | 14")
    asOfDate: Optional[date] = None
    returnDaily: bool = True


class DailyForecast(BaseModel):
    date: date
    yhat: float


class ForecastPerProduct(BaseModel):
    productId: int
    daily: List[DailyForecast]
    sum: float


class ForecastResponse(BaseModel):
    forecasts: List[ForecastPerProduct]
    modelVersion: str = "baseline-0.1"

