from __future__ import annotations

from fastapi import APIRouter, Query

from src.db.sqlite_client import query_correlation_matrix, query_impact_factors, query_scatter_data
from src.models.schemas import CorrelationMatrix, FactorRankItem, ScatterPoint

router = APIRouter(prefix="/api/impact")


@router.get("/factors", response_model=list[FactorRankItem])
def get_factors(warehouseCode: str = Query(...)):
    rows = query_impact_factors(warehouseCode)
    return [
        FactorRankItem(
            rank=r["rank"],
            factorName=r["factor_name"],
            correlation=r["correlation"],
            description=r["description"],
        )
        for r in rows
    ]


@router.get("/correlation", response_model=CorrelationMatrix)
def get_correlation(warehouseCode: str = Query(...)):
    data = query_correlation_matrix(warehouseCode)
    if not data:
        return CorrelationMatrix(factors=[], matrix=[])
    return CorrelationMatrix(factors=data["factors"], matrix=data["matrix"])


FACTOR_COL_MAP = {
    "出库单量": "ob_orders",
    "出库件数": "ob_items",
    "件单比": "item_order_ratio",
    "入库单量": "ib_orders",
    "退货量": "return_orders",
    "出勤人数": "headcount",
    "固定劳务人数": "fixed_count",
    "临时劳务人数": "temp_count",
    "固临比": "fixed_temp_ratio",
    "上架量": "shelf_orders",
}


@router.get("/scatter", response_model=list[ScatterPoint])
def get_scatter(warehouseCode: str = Query(...), factor: str = Query(...)):
    col = FACTOR_COL_MAP.get(factor)
    if not col:
        return []
    rows = query_scatter_data(warehouseCode, col)
    return [
        ScatterPoint(
            date=r["date"],
            factorValue=float(r["factor_value"] or 0),
            workHours=float(r["work_hours"] or 0),
        )
        for r in rows
    ]
