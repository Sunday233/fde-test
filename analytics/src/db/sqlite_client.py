from __future__ import annotations

import json
import logging
import os
import sqlite3
from pathlib import Path

from src.config import settings

logger = logging.getLogger(__name__)

_db_path: str = ""


def _get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(_db_path)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    global _db_path
    _db_path = settings.SQLITE_PATH
    Path(_db_path).parent.mkdir(parents=True, exist_ok=True)
    conn = _get_conn()
    try:
        conn.execute("PRAGMA journal_mode=WAL")
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS daily_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                warehouse_code TEXT NOT NULL,
                warehouse_name TEXT,
                ob_orders INTEGER DEFAULT 0,
                ob_items REAL DEFAULT 0,
                item_order_ratio REAL DEFAULT 0,
                ib_orders INTEGER DEFAULT 0,
                ib_items REAL DEFAULT 0,
                return_orders INTEGER DEFAULT 0,
                shelf_orders INTEGER DEFAULT 0,
                shelf_items REAL DEFAULT 0,
                headcount INTEGER DEFAULT 0,
                total_work_hours REAL DEFAULT 0,
                fixed_count INTEGER DEFAULT 0,
                temp_count INTEGER DEFAULT 0,
                own_count INTEGER DEFAULT 0,
                fixed_temp_ratio REAL DEFAULT 0,
                computed_at TEXT NOT NULL,
                UNIQUE(date, warehouse_code)
            );
            CREATE TABLE IF NOT EXISTS baseline_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                month TEXT NOT NULL,
                warehouse_code TEXT NOT NULL,
                warehouse_name TEXT,
                total_orders INTEGER DEFAULT 0,
                total_items REAL DEFAULT 0,
                total_work_hours REAL DEFAULT 0,
                avg_unit_price REAL DEFAULT 0,
                estimated_fee REAL DEFAULT 0,
                cost_per_order REAL DEFAULT 0,
                cost_per_item REAL DEFAULT 0,
                avg_headcount INTEGER DEFAULT 0,
                working_days INTEGER DEFAULT 0,
                computed_at TEXT NOT NULL,
                UNIQUE(month, warehouse_code)
            );
            CREATE TABLE IF NOT EXISTS impact_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                warehouse_code TEXT NOT NULL,
                factor_name TEXT NOT NULL,
                correlation REAL DEFAULT 0,
                rank INTEGER DEFAULT 0,
                description TEXT,
                matrix_json TEXT,
                sample_count INTEGER DEFAULT 0,
                computed_at TEXT NOT NULL,
                UNIQUE(warehouse_code, factor_name)
            );
        """)
        conn.commit()
        logger.info("SQLite database initialized at %s", _db_path)
    finally:
        conn.close()


def upsert_daily_metrics(records: list[dict]) -> None:
    if not records:
        return
    conn = _get_conn()
    try:
        conn.executemany(
            """INSERT INTO daily_metrics
               (date, warehouse_code, warehouse_name, ob_orders, ob_items,
                item_order_ratio, ib_orders, ib_items, return_orders,
                shelf_orders, shelf_items, headcount, total_work_hours,
                fixed_count, temp_count, own_count, fixed_temp_ratio, computed_at)
               VALUES
               (:date, :warehouse_code, :warehouse_name, :ob_orders, :ob_items,
                :item_order_ratio, :ib_orders, :ib_items, :return_orders,
                :shelf_orders, :shelf_items, :headcount, :total_work_hours,
                :fixed_count, :temp_count, :own_count, :fixed_temp_ratio, :computed_at)
               ON CONFLICT(date, warehouse_code) DO UPDATE SET
                warehouse_name=excluded.warehouse_name,
                ob_orders=excluded.ob_orders, ob_items=excluded.ob_items,
                item_order_ratio=excluded.item_order_ratio,
                ib_orders=excluded.ib_orders, ib_items=excluded.ib_items,
                return_orders=excluded.return_orders,
                shelf_orders=excluded.shelf_orders, shelf_items=excluded.shelf_items,
                headcount=excluded.headcount, total_work_hours=excluded.total_work_hours,
                fixed_count=excluded.fixed_count, temp_count=excluded.temp_count,
                own_count=excluded.own_count, fixed_temp_ratio=excluded.fixed_temp_ratio,
                computed_at=excluded.computed_at""",
            records,
        )
        conn.commit()
        logger.info("Upserted %d daily_metrics records", len(records))
    finally:
        conn.close()


def upsert_baseline_results(records: list[dict]) -> None:
    if not records:
        return
    conn = _get_conn()
    try:
        conn.executemany(
            """INSERT INTO baseline_results
               (month, warehouse_code, warehouse_name, total_orders, total_items,
                total_work_hours, avg_unit_price, estimated_fee,
                cost_per_order, cost_per_item, avg_headcount, working_days, computed_at)
               VALUES
               (:month, :warehouse_code, :warehouse_name, :total_orders, :total_items,
                :total_work_hours, :avg_unit_price, :estimated_fee,
                :cost_per_order, :cost_per_item, :avg_headcount, :working_days, :computed_at)
               ON CONFLICT(month, warehouse_code) DO UPDATE SET
                warehouse_name=excluded.warehouse_name,
                total_orders=excluded.total_orders, total_items=excluded.total_items,
                total_work_hours=excluded.total_work_hours,
                avg_unit_price=excluded.avg_unit_price, estimated_fee=excluded.estimated_fee,
                cost_per_order=excluded.cost_per_order, cost_per_item=excluded.cost_per_item,
                avg_headcount=excluded.avg_headcount, working_days=excluded.working_days,
                computed_at=excluded.computed_at""",
            records,
        )
        conn.commit()
        logger.info("Upserted %d baseline_results records", len(records))
    finally:
        conn.close()


def upsert_impact_results(records: list[dict]) -> None:
    if not records:
        return
    conn = _get_conn()
    try:
        conn.executemany(
            """INSERT INTO impact_results
               (warehouse_code, factor_name, correlation, rank, description,
                matrix_json, sample_count, computed_at)
               VALUES
               (:warehouse_code, :factor_name, :correlation, :rank, :description,
                :matrix_json, :sample_count, :computed_at)
               ON CONFLICT(warehouse_code, factor_name) DO UPDATE SET
                correlation=excluded.correlation, rank=excluded.rank,
                description=excluded.description, matrix_json=excluded.matrix_json,
                sample_count=excluded.sample_count, computed_at=excluded.computed_at""",
            records,
        )
        conn.commit()
        logger.info("Upserted %d impact_results records", len(records))
    finally:
        conn.close()


def query_daily_metrics(warehouse_code: str, month: str) -> list[dict]:
    conn = _get_conn()
    try:
        cur = conn.execute(
            """SELECT * FROM daily_metrics
               WHERE warehouse_code = ? AND date LIKE ?
               ORDER BY date""",
            (warehouse_code, f"{month}%"),
        )
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def query_baseline_results(warehouse_code: str) -> list[dict]:
    conn = _get_conn()
    try:
        cur = conn.execute(
            """SELECT * FROM baseline_results
               WHERE warehouse_code = ? ORDER BY month""",
            (warehouse_code,),
        )
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def query_impact_factors(warehouse_code: str) -> list[dict]:
    conn = _get_conn()
    try:
        cur = conn.execute(
            """SELECT factor_name, correlation, rank, description, sample_count
               FROM impact_results
               WHERE warehouse_code = ? ORDER BY rank""",
            (warehouse_code,),
        )
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def query_correlation_matrix(warehouse_code: str) -> dict | None:
    conn = _get_conn()
    try:
        cur = conn.execute(
            """SELECT matrix_json FROM impact_results
               WHERE warehouse_code = ? AND matrix_json IS NOT NULL
               LIMIT 1""",
            (warehouse_code,),
        )
        row = cur.fetchone()
        if row and row["matrix_json"]:
            return json.loads(row["matrix_json"])
        return None
    finally:
        conn.close()


def query_scatter_data(warehouse_code: str, column_name: str) -> list[dict]:
    """Return daily (date, factor_value, work_hours) for a given warehouse and metric column."""
    # whitelist valid column names to prevent SQL injection
    VALID_COLUMNS = {
        "ob_orders", "ob_items", "item_order_ratio",
        "ib_orders", "headcount", "temp_count",
        "fixed_count", "fixed_temp_ratio",
        "shelf_orders", "return_orders",
    }
    if column_name not in VALID_COLUMNS:
        return []
    conn = _get_conn()
    try:
        cur = conn.execute(
            f"""SELECT date, {column_name} as factor_value, total_work_hours as work_hours
                FROM daily_metrics
                WHERE warehouse_code = ? AND total_work_hours > 0
                ORDER BY date""",
            (warehouse_code,),
        )
        return [dict(row) for row in cur.fetchall()]
    finally:
        conn.close()


def get_last_computed_at() -> str | None:
    conn = _get_conn()
    try:
        cur = conn.execute(
            """SELECT MAX(computed_at) as last FROM (
                SELECT computed_at FROM daily_metrics
                UNION ALL
                SELECT computed_at FROM baseline_results
                UNION ALL
                SELECT computed_at FROM impact_results
            )"""
        )
        row = cur.fetchone()
        return row["last"] if row else None
    finally:
        conn.close()


def check_file_exists() -> bool:
    return os.path.exists(_db_path)
