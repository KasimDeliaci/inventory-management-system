from __future__ import annotations

from datetime import date
from typing import Iterable, List, Optional
import httpx

from ..config import settings


class InventoryClient:
    def __init__(self, base_url: Optional[str] = None, timeout: float = 30.0):
        self.base_url = base_url or settings.INVENTORY_BASE_URL
        self.timeout = timeout

    def _join_product_ids(self, product_ids: Optional[Iterable[int]]) -> list[tuple[str, str]]:
        params: list[tuple[str, str]] = []
        if product_ids:
            for pid in product_ids:
                # Inventory ReportingRequest expects 'productId' as the query key
                params.append(("productId", str(int(pid))))
        return params

    def get_product_day_sales(self, start: date, end: date, product_ids: Optional[Iterable[int]] = None) -> list[dict]:
        """Calls /api/v1/reporting/product-day-sales and returns a list of rows.
        Row shape: {date, productId, salesUnits, offerActiveShare}
        """
        url = f"{self.base_url}/api/v1/reporting/product-day-sales"
        params: list[tuple[str, str]] = [("from", start.isoformat()), ("to", end.isoformat())]
        params += self._join_product_ids(product_ids)
        with httpx.Client(timeout=self.timeout) as client:
            resp = client.get(url, params=params)
            resp.raise_for_status()
            return resp.json()

    def get_product_day_promo(self, start: date, end: date, product_ids: Optional[Iterable[int]] = None) -> list[dict]:
        """Calls /api/v1/reporting/product-day-promo and returns a list of rows.
        Row shape: {date, productId, promoPct}
        """
        url = f"{self.base_url}/api/v1/reporting/product-day-promo"
        params: list[tuple[str, str]] = [("from", start.isoformat()), ("to", end.isoformat())]
        params += self._join_product_ids(product_ids)
        with httpx.Client(timeout=self.timeout) as client:
            resp = client.get(url, params=params)
            resp.raise_for_status()
            return resp.json()

    def get_day_offer_stats(self, start: date, end: date) -> list[dict]:
        """Calls /api/v1/reporting/day-offer-stats and returns a list of rows.
        Row shape: {date, activeOffersCount, offerAvgPct, offerMaxPct}
        """
        url = f"{self.base_url}/api/v1/reporting/day-offer-stats"
        params: list[tuple[str, str]] = [("from", start.isoformat()), ("to", end.isoformat())]
        with httpx.Client(timeout=self.timeout) as client:
            resp = client.get(url, params=params)
            resp.raise_for_status()
            return resp.json()

    def get_product(self, product_id: int) -> dict:
        """Fetch a single product. Useful to read unitOfMeasure for rounding decisions.
        Response shape includes unitOfMeasure.
        """
        url = f"{self.base_url}/api/v1/products/{int(product_id)}"
        with httpx.Client(timeout=self.timeout) as client:
            resp = client.get(url)
            resp.raise_for_status()
            return resp.json()

    def get_products_uom(self, product_ids: Iterable[int]) -> dict[int, str]:
        """Return a mapping of productId -> unitOfMeasure for provided ids.
        Falls back silently on failures per product.
        """
        out: dict[int, str] = {}
        for pid in set(int(p) for p in product_ids or []):
            try:
                p = self.get_product(pid)
                uom = p.get("unitOfMeasure") or p.get("unit_of_measure")
                if isinstance(uom, str):
                    out[pid] = uom
            except Exception:
                # ignore fetch errors per product
                pass
        return out
