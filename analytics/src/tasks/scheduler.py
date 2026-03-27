from __future__ import annotations

import logging
import time

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.interval import IntervalTrigger

from src.config import settings

logger = logging.getLogger(__name__)

_scheduler: BackgroundScheduler | None = None


def run_precompute() -> None:
    """Execute the full precompute pipeline: daily_metrics → baseline → impact."""
    from src.services.baseline_service import compute_baseline
    from src.services.daily_metrics import compute_daily_metrics
    from src.services.impact_service import compute_all_impacts

    logger.info("Precompute started")
    start = time.time()

    try:
        dm_count = compute_daily_metrics()
        logger.info("  daily_metrics: %d records", dm_count)
    except Exception:
        logger.exception("Failed to compute daily_metrics")
        return

    try:
        bl_count = compute_baseline()
        logger.info("  baseline_results: %d records", bl_count)
    except Exception:
        logger.exception("Failed to compute baseline")

    try:
        im_count = compute_all_impacts()
        logger.info("  impact_results: %d records", im_count)
    except Exception:
        logger.exception("Failed to compute impact")

    elapsed = round(time.time() - start, 2)
    logger.info("Precompute finished in %.2fs", elapsed)


def start_scheduler() -> None:
    global _scheduler
    _scheduler = BackgroundScheduler()
    _scheduler.add_job(
        run_precompute,
        trigger=IntervalTrigger(hours=settings.PRECOMPUTE_INTERVAL_HOURS),
        id="precompute",
        max_instances=1,
        replace_existing=True,
    )
    _scheduler.start()
    logger.info("Scheduler started (interval=%dh)", settings.PRECOMPUTE_INTERVAL_HOURS)


def stop_scheduler() -> None:
    global _scheduler
    if _scheduler:
        _scheduler.shutdown(wait=False)
        _scheduler = None
        logger.info("Scheduler stopped")
