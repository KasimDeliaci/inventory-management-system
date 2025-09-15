from datetime import date as Date, datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class TrainRequest(BaseModel):
    scope: str = Field(default="global", description="Training scope: 'global' for all products or 'product' for specific product", example="global")
    productId: Optional[int] = Field(default=None, description="Required when scope='product'. Product ID to train model for", example=1001)
    trainWindowDays: int = Field(default=1095, description="Number of days of historical data to use for training (36 months default)", example=1095)
    algorithm: str = Field(default="xgb_three", description="ML algorithm to use", example="xgb_three")
    hyperparams: dict = Field(default_factory=dict, description="XGBoost hyperparameters", example={"learning_rate": 0.1, "max_depth": 6, "n_estimators": 800})


class TrainResponse(BaseModel):
    taskId: str = Field(description="Unique task identifier for tracking training progress", example="123e4567-e89b-12d3-a456-426614174000")
    status: str = Field(default="queued", description="Current training status", example="queued")
    statusUrl: str = Field(description="URL to check training status", example="/train/status/123e4567-e89b-12d3-a456-426614174000")


class ForecastRequest(BaseModel):
    productIds: List[int] = Field(description="List of product IDs to forecast", example=[1001, 1002, 1003])
    horizonDays: int = Field(default=7, description="Forecast horizon in days. Must be 1, 7, or 14", example=7)
    asOfDate: Optional[Date] = Field(default=None, description="Date to forecast from (defaults to today)", example="2025-06-30")
    returnDaily: bool = Field(default=True, description="Whether to return daily breakdown or just totals", example=True)


class DailyForecast(BaseModel):
    date: Date = Field(description="Forecast date", example="2025-07-01")
    yhat: float = Field(description="Predicted sales units for this date", example=45.2)


class PredictionInterval(BaseModel):
    lowerBound: float = Field(description="Lower bound of 80% confidence interval", example=85.3)
    upperBound: float = Field(description="Upper bound of 80% confidence interval", example=156.7)


class Confidence(BaseModel):
    score: int = Field(description="Confidence score (0-100)", example=75)
    level: str = Field(description="Confidence level description", example="medium")
    factors: Dict[str, Any] = Field(description="Factors affecting confidence", example={"historical_pattern": "stable", "seasonal_effects": "moderate"})
    recommendation: str = Field(description="Recommendation based on confidence", example="Reliable forecast with moderate seasonal variation")


class ForecastPerProduct(BaseModel):
    productId: int = Field(description="Product ID", example=1001)
    daily: List[DailyForecast] = Field(description="Daily forecast breakdown (empty if returnDaily=false)")
    sum: float = Field(description="Total predicted sales over the horizon", example=289.4)
    predictionInterval: Optional[PredictionInterval] = Field(default=None, description="80% confidence interval bounds")
    confidence: Optional[Confidence] = Field(default=None, description="Confidence assessment for this forecast")


class ForecastResponse(BaseModel):
    forecasts: List[ForecastPerProduct] = Field(description="Forecast results per product")
    modelVersion: str = Field(default="xgb_three-latest", description="Model version used", example="xgb_three-20250913125620")
    modelType: Optional[str] = Field(default="xgb_three", description="Model algorithm type", example="xgb_three")
    generatedAt: datetime = Field(default_factory=lambda: datetime.utcnow(), description="Timestamp when forecast was generated")
    forecastId: int = Field(description="Forecast run id of this response", example=123)


# History schemas
class ForecastRunSummary(BaseModel):
    forecastId: int = Field(description="Forecast header id", example=123)
    asOfDate: Date = Field(description="As-of date used for this forecast", example="2025-06-30")
    horizonDays: int = Field(description="Horizon length in days (1|7|14)", example=7)
    requestedAt: datetime = Field(description="Timestamp when this forecast was computed", example="2025-09-13T09:00:00Z")
    modelVersion: Optional[str] = Field(default=None, description="Version label of the model used", example="xgb_three-20250913125620")
    sumYhat: float = Field(description="Sum of daily yhat for this product within the run", example=58.0)


class ForecastHistoryItem(BaseModel):
    date: Date = Field(description="Forecast date", example="2025-07-01")
    yhat: float = Field(description="Predicted units for this date (rounded for countable UoMs)", example=8)
    lower: Optional[float] = Field(default=None, description="Per-day lower bound (optional)", example=5)
    upper: Optional[float] = Field(default=None, description="Per-day upper bound (optional)", example=12)
    confidence: Optional[Dict[str, Any]] = Field(default=None, description="Confidence payload for this item (optional)", example={"score": 62, "level": "medium"})


class ForecastRunDetail(BaseModel):
    forecastId: int = Field(description="Forecast header id", example=123)
    productId: int = Field(description="Product id for these items", example=1001)
    asOfDate: Date = Field(description="As-of date the forecast was based on", example="2025-06-30")
    horizonDays: int = Field(description="Horizon length in days (1|7|14)", example=7)
    requestedAt: datetime = Field(description="Computation time of the run", example="2025-09-13T09:00:00Z")
    modelVersion: Optional[str] = Field(default=None, description="Model version label used", example="xgb_three-20250913125620")
    items: List[ForecastHistoryItem] = Field(description="Daily forecast items for the selected product and run")
        