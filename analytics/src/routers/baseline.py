from __future__ import annotations

from fastapi import APIRouter, Query

from src.db.sqlite_client import query_baseline_results, query_daily_metrics
from src.models.schemas import BaselineItem, DailyMetricItem

router = APIRouter(prefix="/api/baseline")


@router.get("/monthly", response_model=list[BaselineItem])
def get_monthly(warehouseCode: str | None = Query(default=None)):
    if warehouseCode:
        rows = query_baseline_results(warehouseCode)
    else:
        # Query all warehouses
        from src.services.daily_metrics import HOUSE_MAP
        rows = []
        for wh_code in HOUSE_MAP:
            rows.extend(query_baseline_results(wh_code))
        rows.sort(key=lambda r: (r["month"], r["warehouse_code"]), reverse=True)

    return [
        BaselineItem(
            month=r["month"],
            warehouseCode=r["warehouse_code"],
            warehouseName=r["warehouse_name"] or "",
            totalOrders=r["total_orders"],
            totalItems=r["total_items"],
            totalWorkHours=r["total_work_hours"],
            avgUnitPrice=r["avg_unit_price"],
            estimatedFee=r["estimated_fee"],
            costPerOrder=r["cost_per_order"],
            costPerItem=r["cost_per_item"],
            avgHeadcount=r["avg_headcount"],
            workingDays=r["working_days"],
            computedAt=r["computed_at"],
        )
        for r in rows
    ]


@router.get("/daily-metrics", response_model=list[DailyMetricItem])
def get_daily_metrics(
    warehouseCode: str = Query(...),
    month: str = Query(..., description="yyyy-MM format"),
):
    rows = query_daily_metrics(warehouseCode, month)
    return [
        DailyMetricItem(
            date=r["date"],
            warehouseCode=r["warehouse_code"],
            warehouseName=r.get("warehouse_name"),
            obOrders=r["ob_orders"],
            obItems=r["ob_items"],
            itemOrderRatio=r["item_order_ratio"],
            ibOrders=r["ib_orders"],
            ibItems=r["ib_items"],
            returnOrders=r["return_orders"],
            shelfOrders=r["shelf_orders"],
            shelfItems=r["shelf_items"],
            headcount=r["headcount"],
            totalWorkHours=r["total_work_hours"],
            fixedCount=r["fixed_count"],
            tempCount=r["temp_count"],
            ownCount=r["own_count"],
            fixedTempRatio=r["fixed_temp_ratio"],
        )
        for r in rows
    ]
