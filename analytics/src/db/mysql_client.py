from __future__ import annotations

import logging
import queue
from contextlib import contextmanager

import pymysql
from pymysql.cursors import DictCursor

from src.config import settings

logger = logging.getLogger(__name__)

_pool: queue.Queue | None = None
_POOL_SIZE = 5


def _create_connection() -> pymysql.Connection:
    conn = pymysql.connect(
        host=settings.MYSQL_HOST,
        port=settings.MYSQL_PORT,
        user=settings.MYSQL_USER,
        password=settings.MYSQL_PASSWORD,
        database=settings.MYSQL_DATABASE,
        charset="utf8mb4",
        cursorclass=DictCursor,
        connect_timeout=10,
    )
    with conn.cursor() as cur:
        cur.execute("SET SESSION TRANSACTION READ ONLY")
    return conn


def init_pool() -> None:
    global _pool
    pool = queue.Queue(maxsize=_POOL_SIZE)
    for _ in range(_POOL_SIZE):
        pool.put(_create_connection())
    _pool = pool
    logger.info("MySQL connection pool initialized (size=%d)", _POOL_SIZE)


def close_pool() -> None:
    global _pool
    if _pool is None:
        return
    while not _pool.empty():
        try:
            conn = _pool.get_nowait()
            conn.close()
        except Exception:
            pass
    _pool = None
    logger.info("MySQL connection pool closed")


@contextmanager
def get_connection():
    if _pool is None:
        raise ConnectionError("MySQL connection pool not initialized")
    try:
        conn = _pool.get(timeout=30)
    except queue.Empty:
        raise ConnectionError("MySQL connection pool exhausted (timeout=30s)")
    try:
        conn.ping(reconnect=True)
        yield conn
    except pymysql.OperationalError:
        # Connection broken, replace with a new one
        try:
            conn.close()
        except Exception:
            pass
        conn = _create_connection()
        yield conn
    finally:
        try:
            _pool.put_nowait(conn)
        except queue.Full:
            conn.close()


def check_connectivity() -> bool:
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                return True
    except Exception:
        return False
