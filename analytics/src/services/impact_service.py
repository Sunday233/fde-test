from __future__ import annotations

import json
import logging
import sqlite3
from datetime import datetime

from src.config import settings
from src.db.sqlite_client import upsert_impact_results
from src.services.correlation import correlation_matrix, pearson

logger = logging.getLogger(__name__)

FACTORS = [
    ("出库单量", "ob_orders"),
    ("出库件数", "ob_items"),
    ("件单比", "item_order_ratio"),
    ("入库单量", "ib_orders"),
    ("退货量", "return_orders"),
    ("出勤人数", "headcount"),
    ("固定劳务人数", "fixed_count"),
    ("临时劳务人数", "temp_count"),
    ("固临比", "fixed_temp_ratio"),
    ("上架量", "shelf_orders"),
]

MIN_SAMPLES = 5


def _describe(r: float) -> str:
    abs_r = abs(r)
    if abs_r > 0.7:
        return "核心驱动因子"
    if abs_r > 0.4:
        return "重要影响因子"
    if abs_r > 0.2:
        return "次要影响因子"
    return "影响不显著"


def _load_daily_metrics(warehouse_code: str) -> list[dict]:
    """Load valid daily_metrics records for a warehouse."""
    db_path = settings.SQLITE_PATH
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.execute(
            """SELECT * FROM daily_metrics
               WHERE warehouse_code = ? AND total_work_hours > 0 AND ob_orders > 0""",
            (warehouse_code,),
        )
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def compute_impact(warehouse_code: str) -> int:
    """Compute impact factors for a warehouse and write to impact_results.

    Returns the number of records written.
    """
    logger.info("Computing impact factors for warehouse %s", warehouse_code)

    rows = _load_daily_metrics(warehouse_code)
    if len(rows) < MIN_SAMPLES:
        logger.warning("Warehouse %s has only %d valid samples (min %d), skipping",
                        warehouse_code, len(rows), MIN_SAMPLES)
        return 0

    # Extract target variable
    target = [float(r["total_work_hours"]) for r in rows]

    # Compute per-factor Pearson correlation with target
    factor_results = []
    factor_data: dict[str, list[float]] = {}
    for display_name, col_name in FACTORS:
        values = [float(r[col_name]) for r in rows]
        factor_data[display_name] = values
        r = pearson(values, target)
        factor_results.append((display_name, round(r, 4)))

    # Sort by |r| descending
    factor_results.sort(key=lambda x: abs(x[1]), reverse=True)

    # Build 10x10 correlation matrix
    factors_list, matrix = correlation_matrix(factor_data)

    now = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    matrix_json = json.dumps({"factors": factors_list, "matrix": matrix}, ensure_ascii=False)

    records = []
    for rank, (name, r) in enumerate(factor_results, start=1):
        records.append({
            "warehouse_code": warehouse_code,
            "factor_name": name,
            "correlation": r,
            "rank": rank,
            "description": _describe(r),
            "matrix_json": matrix_json if rank == 1 else None,
            "sample_count": len(rows),
            "computed_at": now,
        })

    upsert_impact_results(records)
    logger.info("Impact factors computed for %s: %d factors, %d samples",
                warehouse_code, len(records), len(rows))
    return len(records)


def compute_all_impacts() -> int:
    """Compute impact factors for all known warehouses."""
    from src.services.daily_metrics import HOUSE_MAP
    total = 0
    for wh_code in HOUSE_MAP:
        total += compute_impact(wh_code)
    return total
