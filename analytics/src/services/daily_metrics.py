from __future__ import annotations

import logging
from collections import defaultdict
from datetime import datetime

from src.db.mysql_client import get_connection
from src.db.sqlite_client import upsert_daily_metrics

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


def _query_ob_daily() -> list[dict]:
    """出库日维度"""
    sql = """
        SELECT DATE_FORMAT(`创建时间`, '%Y-%m-%d') AS ymd,
               `库房编码` AS warehouse_code,
               COUNT(*) AS ob_orders,
               SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS ob_items
        FROM `出库单表`
        WHERE `状态` != '撤单' AND `创建时间` IS NOT NULL
        GROUP BY ymd, warehouse_code
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()


def _query_ib_daily() -> list[dict]:
    """入库日维度"""
    sql = """
        SELECT DATE_FORMAT(`创单时间`, '%Y-%m-%d') AS ymd,
               `库房编码` AS warehouse_code,
               COUNT(*) AS ib_orders,
               SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS ib_items
        FROM `入库单表`
        WHERE `状态` = '正常' AND `创单时间` IS NOT NULL AND `库房编码` IS NOT NULL
        GROUP BY ymd, warehouse_code
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()


def _query_attendance_daily() -> list[dict]:
    """出勤日维度（返回仓库名称，需映射）"""
    sql = """
        SELECT DATE_FORMAT(`考勤日期`, '%Y-%m-%d') AS ymd,
               `库房` AS warehouse_name,
               COUNT(DISTINCT `员工编码`) AS headcount,
               SUM(CAST(`工作时长` AS DECIMAL(10,2))) AS total_work_minutes,
               COUNT(DISTINCT CASE WHEN `员工类型` = '长期劳务' THEN `员工编码` END) AS fixed_count,
               COUNT(DISTINCT CASE WHEN `员工类型` = '临时劳务' THEN `员工编码` END) AS temp_count,
               COUNT(DISTINCT CASE WHEN `员工类型` = '自有人员' THEN `员工编码` END) AS own_count
        FROM `出勤统计表`
        GROUP BY ymd, `库房`
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()


def _query_shelf_daily() -> list[dict]:
    """上架日维度"""
    sql = """
        SELECT DATE_FORMAT(`创建时间`, '%Y-%m-%d') AS ymd,
               `库房编码` AS warehouse_code,
               COUNT(*) AS shelf_orders,
               SUM(CAST(`上架单总数量` AS DECIMAL(20,2))) AS shelf_items
        FROM `上架单表`
        GROUP BY ymd, warehouse_code
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()


def _query_return_daily() -> list[dict]:
    """退货日维度"""
    sql = """
        SELECT DATE_FORMAT(i.`创单时间`, '%Y-%m-%d') AS ymd,
               i.`库房编码` AS warehouse_code,
               COUNT(*) AS return_orders
        FROM `退货信息表` r
        JOIN `入库单表` i ON r.`入库单号` = i.`入库单号`
        WHERE i.`状态` = '正常' AND i.`创单时间` IS NOT NULL AND i.`库房编码` IS NOT NULL
        GROUP BY ymd, warehouse_code
    """
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            return cur.fetchall()


def compute_daily_metrics() -> int:
    """Compute daily metrics from all MySQL sources and write to SQLite.

    Returns the number of records written.
    """
    logger.info("Computing daily metrics...")

    # Collect all data sources
    ob_data = _query_ob_daily()
    ib_data = _query_ib_daily()
    attendance_data = _query_attendance_daily()
    shelf_data = _query_shelf_daily()
    return_data = _query_return_daily()

    # Merge by (ymd, warehouse_code)
    merged: dict[tuple[str, str], dict] = defaultdict(lambda: {
        "ob_orders": 0, "ob_items": 0.0,
        "ib_orders": 0, "ib_items": 0.0,
        "headcount": 0, "total_work_hours": 0.0,
        "fixed_count": 0, "temp_count": 0, "own_count": 0,
        "shelf_orders": 0, "shelf_items": 0.0,
        "return_orders": 0,
    })

    for row in ob_data:
        key = (row["ymd"], row["warehouse_code"])
        merged[key]["ob_orders"] = int(row["ob_orders"] or 0)
        merged[key]["ob_items"] = float(row["ob_items"] or 0)

    for row in ib_data:
        key = (row["ymd"], row["warehouse_code"])
        merged[key]["ib_orders"] = int(row["ib_orders"] or 0)
        merged[key]["ib_items"] = float(row["ib_items"] or 0)

    for row in attendance_data:
        wh_name = row["warehouse_name"]
        wh_code = HOUSE_NAME_TO_ID.get(wh_name)
        if not wh_code:
            continue
        key = (row["ymd"], wh_code)
        merged[key]["headcount"] = int(row["headcount"] or 0)
        minutes = float(row["total_work_minutes"] or 0)
        merged[key]["total_work_hours"] = round(minutes / 60, 2)
        merged[key]["fixed_count"] = int(row["fixed_count"] or 0)
        merged[key]["temp_count"] = int(row["temp_count"] or 0)
        merged[key]["own_count"] = int(row["own_count"] or 0)

    for row in shelf_data:
        key = (row["ymd"], row["warehouse_code"])
        merged[key]["shelf_orders"] = int(row["shelf_orders"] or 0)
        merged[key]["shelf_items"] = float(row["shelf_items"] or 0)

    for row in return_data:
        key = (row["ymd"], row["warehouse_code"])
        merged[key]["return_orders"] = int(row["return_orders"] or 0)

    # Build records
    now = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    records = []
    for (ymd, wh_code), vals in merged.items():
        ob_orders = vals["ob_orders"]
        ob_items = vals["ob_items"]
        temp_count = vals["temp_count"]
        fixed_count = vals["fixed_count"]
        records.append({
            "date": ymd,
            "warehouse_code": wh_code,
            "warehouse_name": HOUSE_MAP.get(wh_code, ""),
            "ob_orders": ob_orders,
            "ob_items": ob_items,
            "item_order_ratio": round(ob_items / ob_orders, 4) if ob_orders > 0 else 0.0,
            "ib_orders": vals["ib_orders"],
            "ib_items": vals["ib_items"],
            "return_orders": vals["return_orders"],
            "shelf_orders": vals["shelf_orders"],
            "shelf_items": vals["shelf_items"],
            "headcount": vals["headcount"],
            "total_work_hours": vals["total_work_hours"],
            "fixed_count": fixed_count,
            "temp_count": temp_count,
            "own_count": vals["own_count"],
            "fixed_temp_ratio": round(fixed_count / temp_count, 4) if temp_count > 0 else 0.0,
            "computed_at": now,
        })

    upsert_daily_metrics(records)
    logger.info("Daily metrics computed: %d records", len(records))
    return len(records)
