from fastapi import APIRouter

from src.db.mysql_client import check_connectivity
from src.db.sqlite_client import check_file_exists, get_last_computed_at
from src.models.schemas import HealthResponse

router = APIRouter(prefix="/api")


@router.get("/health", response_model=HealthResponse)
def health():
    mysql_ok = check_connectivity()
    sqlite_ok = check_file_exists()
    last = get_last_computed_at()
    status = "healthy" if mysql_ok and sqlite_ok else "degraded"
    return HealthResponse(
        status=status,
        mysql=mysql_ok,
        sqlite=sqlite_ok,
        last_precompute=last,
        version="0.1.0",
    )
