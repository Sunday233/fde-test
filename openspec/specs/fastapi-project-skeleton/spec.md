## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: FastAPI 项目骨架

系统 SHALL 在 `wh-op-platform/analytics/` 目录下创建完整的 FastAPI 项目结构。

**项目配置文件**:
- `pyproject.toml` — 项目元数据、Python 版本要求（≥3.11）、依赖声明
- `requirements.txt` — pip 可直接安装的依赖列表（含版本锁定）
- 依赖包：fastapi、uvicorn[standard]、pymysql、pydantic、apscheduler

**目录结构**:
```
analytics/
├── pyproject.toml
├── requirements.txt
├── Dockerfile
└── src/
    ├── main.py           # FastAPI 应用入口 + lifespan 事件
    ├── config.py          # 环境变量配置（Pydantic BaseSettings）
    ├── db/
    │   ├── __init__.py
    │   ├── mysql_client.py
    │   └── sqlite_client.py
    ├── routers/
    │   ├── __init__.py
    │   ├── health.py
    │   ├── impact.py
    │   └── baseline.py
    ├── services/
    │   ├── __init__.py
    │   ├── daily_metrics.py
    │   ├── baseline_service.py
    │   ├── impact_service.py
    │   └── correlation.py
    ├── models/
    │   ├── __init__.py
    │   └── schemas.py
    └── tasks/
        ├── __init__.py
        └── scheduler.py
```

**入口文件 `main.py`**:
- 创建 FastAPI 应用实例，设定 `title="WH-OP Analytics Service"`
- 注册所有 router（health、impact、baseline）
- 使用 `lifespan` 上下文管理器处理启动/关闭事件
- 启动时初始化 MySQL 连接池、SQLite 数据库、触发首次预计算
- 关闭时释放 MySQL 连接池、停止调度器

**配置模块 `config.py`**:
- 使用 Pydantic `BaseSettings` 从环境变量读取配置
- 配置项：`MYSQL_HOST`、`MYSQL_PORT`（默认 3306）、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE`（默认 `wh_op_baseline`）、`SQLITE_PATH`（默认 `./data/results.db`）、`PRECOMPUTE_INTERVAL_HOURS`（默认 6）

#### Scenario: 项目可通过 uvicorn 启动

- **WHEN** 执行 `cd analytics && uvicorn src.main:app --host 0.0.0.0 --port 8000`
- **THEN** 服务在 8000 端口启动，控制台输出 "Application startup complete"，可访问 `/docs` 查看 OpenAPI 文档

#### Scenario: 环境变量注入配置

- **WHEN** 设置环境变量 `MYSQL_HOST=10.126.50.199` 后启动服务
- **THEN** `config.settings.MYSQL_HOST` 的值为 `10.126.50.199`
