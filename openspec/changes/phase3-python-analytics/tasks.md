## 1. 项目骨架与配置

- [ ] 1.1 创建 `analytics/` 目录结构（src/、db/、routers/、services/、models/、tasks/ 及 `__init__.py`）
- [ ] 1.2 创建 `requirements.txt`：fastapi、uvicorn[standard]、pymysql、pydantic-settings、apscheduler
- [ ] 1.3 创建 `pyproject.toml`：项目元数据、Python ≥3.11
- [ ] 1.4 创建 `src/config.py`：Pydantic BaseSettings 读取环境变量（MYSQL_HOST/PORT/USER/PASSWORD/DATABASE、SQLITE_PATH、PRECOMPUTE_INTERVAL_HOURS）

## 2. 数据库连接层

- [ ] 2.1 创建 `src/db/mysql_client.py`：pymysql 连接池（Queue 实现，池大小 5，DictCursor，read_only），上下文管理器 `get_connection()` — 操作表：全部 19 张 MySQL 原始表（只读）
- [ ] 2.2 创建 `src/db/sqlite_client.py`：SQLite 存储模块，`init_db()` 创建三张表（daily_metrics、baseline_results、impact_results）并启用 WAL 模式；提供 upsert 和 query 方法

## 3. Pydantic 数据模型

- [ ] 3.1 创建 `src/models/schemas.py`：定义 FactorRankItem（rank, factorName, correlation, description）、CorrelationMatrix（factors, matrix）、BaselineItem、DailyMetricItem、HealthResponse 等 Pydantic 模型，JSON alias 使用 camelCase

## 4. 核心分析服务

- [ ] 4.1 创建 `src/services/correlation.py`：纯 Python Pearson 相关系数实现，`pearson(x, y)` 和 `correlation_matrix(data)` 函数
- [ ] 4.2 创建 `src/services/daily_metrics.py`：从 MySQL 查询 5 个数据源（出库单表、入库单表、出勤统计表、上架单表、退货信息表+入库单表 JOIN），按 (date, warehouse_code) 合并，写入 SQLite daily_metrics 表
- [ ] 4.3 创建 `src/services/baseline_service.py`：从 SQLite daily_metrics 按月聚合，从 MySQL 报价信息表获取加权均价，计算月度费用（公式：总工时 × 均价 × 1.06），写入 baseline_results 表
- [ ] 4.4 创建 `src/services/impact_service.py`：从 SQLite daily_metrics 查询有效样本，调用 correlation.py 计算 10 个因素的 Pearson 相关系数和 10×10 矩阵，写入 impact_results 表

## 5. 预计算调度

- [ ] 5.1 创建 `src/tasks/scheduler.py`：APScheduler BackgroundScheduler，串行执行 daily_metrics → baseline → impact，max_instances=1，IntervalTrigger 每 N 小时触发

## 6. API 路由层

- [ ] 6.1 创建 `src/routers/health.py`：`GET /api/health` 返回 MySQL/SQLite 连通性、last_precompute、version
- [ ] 6.2 创建 `src/routers/impact.py`：`GET /api/impact/factors?warehouseCode={code}` 和 `GET /api/impact/correlation?warehouseCode={code}`，从 SQLite 查询预计算结果，响应格式匹配 FactorRankVO / CorrelationMatrixVO
- [ ] 6.3 创建 `src/routers/baseline.py`：`GET /api/baseline/monthly?warehouseCode={code}` 和 `GET /api/baseline/daily-metrics?warehouseCode={code}&month={yyyy-MM}`

## 7. 应用入口

- [ ] 7.1 创建 `src/main.py`：FastAPI 应用实例、注册 router、lifespan 管理（启动时 init_db + start_scheduler + run_precompute，关闭时 stop_scheduler）

## 8. Docker 与验证

- [ ] 8.1 创建 `analytics/Dockerfile`：python:3.11-slim 基础镜像，pip install requirements.txt，uvicorn 启动，HEALTHCHECK
- [ ] 8.2 本地验证：`pip install -r requirements.txt && uvicorn src.main:app` 启动成功，访问 `/api/health` 返回 200，访问 `/docs` 显示 OpenAPI 文档
