from __future__ import annotations

from pydantic import BaseModel, Field


class FactorRankItem(BaseModel):
    rank: int
    factor_name: str = Field(alias="factorName")
    correlation: float
    description: str

    model_config = {"populate_by_name": True}


class CorrelationMatrix(BaseModel):
    factors: list[str]
    matrix: list[list[float]]


class ScatterPoint(BaseModel):
    date: str
    factor_value: float = Field(alias="factorValue")
    work_hours: float = Field(alias="workHours")

    model_config = {"populate_by_name": True}


class BaselineItem(BaseModel):
    month: str
    warehouse_code: str = Field(alias="warehouseCode")
    warehouse_name: str = Field(alias="warehouseName")
    total_orders: int = Field(alias="totalOrders")
    total_items: float = Field(alias="totalItems")
    total_work_hours: float = Field(alias="totalWorkHours")
    avg_unit_price: float = Field(alias="avgUnitPrice")
    estimated_fee: float = Field(alias="estimatedFee")
    cost_per_order: float = Field(alias="costPerOrder")
    cost_per_item: float = Field(alias="costPerItem")
    avg_headcount: int = Field(alias="avgHeadcount")
    working_days: int = Field(alias="workingDays")
    computed_at: str = Field(alias="computedAt")

    model_config = {"populate_by_name": True}


class DailyMetricItem(BaseModel):
    date: str
    warehouse_code: str = Field(alias="warehouseCode")
    warehouse_name: str | None = Field(default=None, alias="warehouseName")
    ob_orders: int = Field(alias="obOrders")
    ob_items: float = Field(alias="obItems")
    item_order_ratio: float = Field(alias="itemOrderRatio")
    ib_orders: int = Field(alias="ibOrders")
    ib_items: float = Field(alias="ibItems")
    return_orders: int = Field(alias="returnOrders")
    shelf_orders: int = Field(alias="shelfOrders")
    shelf_items: float = Field(alias="shelfItems")
    headcount: int
    total_work_hours: float = Field(alias="totalWorkHours")
    fixed_count: int = Field(alias="fixedCount")
    temp_count: int = Field(alias="tempCount")
    own_count: int = Field(alias="ownCount")
    fixed_temp_ratio: float = Field(alias="fixedTempRatio")

    model_config = {"populate_by_name": True}


class HealthResponse(BaseModel):
    status: str
    mysql: bool
    sqlite: bool
    last_precompute: str | None = Field(default=None, alias="lastPrecompute")
    version: str

    model_config = {"populate_by_name": True}
