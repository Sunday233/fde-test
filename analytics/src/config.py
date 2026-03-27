from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    MYSQL_HOST: str = "10.126.50.199"
    MYSQL_PORT: int = 3306
    MYSQL_USER: str = "fdeuser"
    MYSQL_PASSWORD: str = "FDE2026!"
    MYSQL_DATABASE: str = "wh_op_baseline"

    SQLITE_PATH: str = "./data/results.db"

    PRECOMPUTE_INTERVAL_HOURS: int = 6

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
