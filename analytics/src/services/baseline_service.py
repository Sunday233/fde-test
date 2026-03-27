from __future__ import annotations

import logging
import sqlite3
from collections import defaultdict
from datetime import datetime

from src.config import settings
from src.db.mysql_client import get_connection
from src.db.sqlite_client import upsert_baseline_results

logger = logging.getLogger(__name__)

HOUSE_MAP = {
    "12000004": "天津武清佩森A仓",
    "32050005": "常熟高新正创B仓",
}

HOUSE_NAME_TO_ID = {
    "天津武清佩森A仓": "12000004",
    "常熟高新正创B仓": "32050005",
    "常熟高新正创仓": "32050005",
}


def _query_avg_unit_price() -> dict[str, float]:
    """Query weighted average unit price from 报价信息表, returns {warehouse_code: avg_price}."""
    sql = """
        SELECT `库房名称`, AVG(`供应商结算单价`) AS avg_price
        FROM `报价信息表`
        WHERE `报价状态` = '正常'
        GROUP BY `库房名称`
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()

    result = {}
    for row in rows:
        wh_name = row["库房名称"]
        wh_code = HOUSE_NAME_TO_ID.get(wh_name)
        if wh_code:
            result[wh_code] = float(row["avg_price"] or 0)
    return result


def _aggregate_monthly_from_sqlite() -> list[dict]:
    """Aggregate daily_metrics into monthly groups."""
    db_path = settings.SQLITE_PATH
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        cur = conn.execute("""
            SELECT
                SUBSTR(date, 1, 7) AS month,
                warehouse_code,
                SUM(ob_orders) AS total_orders,
                SUM(ob_items) AS total_items,
                SUM(total_work_hours) AS total_work_hours,
                ROUND(AVG(headcount)) AS avg_headcount,
                COUNT(DISTINCT CASE WHEN ob_orders > 0 THEN date END) AS working_days
            FROM daily_metrics
            GROUP BY month, warehouse_code
        """)
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def compute_baseline() -> int:
    """Compute monthly baseline from daily_metrics and write to baseline_results.

    Returns the number of records written.
    """
    logger.info("Computing baseline results...")

    # Step 1: aggregate monthly from SQLite
    monthly = _aggregate_monthly_from_sqlite()
    if not monthly:
        logger.info("No daily_metrics data, skipping baseline computation")
        return 0

    # Step 2: get avg unit prices from MySQL
    price_map = _query_avg_unit_price()

    # Step 3: compute fee and build records
    now = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    records = []
    for row in monthly:
        wh_code = row["warehouse_code"]
        total_work_hours = float(row["total_work_hours"] or 0)
        total_orders = int(row["total_orders"] or 0)
        total_items = float(row["total_items"] or 0)
        avg_unit_price = price_map.get(wh_code, 0.0)
        estimated_fee = round(total_work_hours * avg_unit_price * 1.06, 2)
        records.append({
            "month": row["month"],
            "warehouse_code": wh_code,
            "warehouse_name": HOUSE_MAP.get(wh_code, ""),
            "total_orders": total_orders,
            "total_items": total_items,
            "total_work_hours": total_work_hours,
            "avg_unit_price": avg_unit_price,
            "estimated_fee": estimated_fee,
            "cost_per_order": round(estimated_fee / total_orders, 2) if total_orders > 0 else 0.0,
            "cost_per_item": round(estimated_fee / total_items, 2) if total_items > 0 else 0.0,
            "avg_headcount": int(row["avg_headcount"] or 0),
            "working_days": int(row["working_days"] or 0),
            "computed_at": now,
        })

    upsert_baseline_results(records)
    logger.info("Baseline results computed: %d records", len(records))
    return len(records)
