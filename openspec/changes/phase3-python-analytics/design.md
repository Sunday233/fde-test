## Context

Phase 0–2 已完成：项目初始化、Spring Boot 后端骨架和 API 层均已就绪。后端 `AnalyticsClient` 已实现，通过 RestClient 调用 Python 服务的 `/api/impact/factors` 和 `/api/impact/correlation` 端点（失败时优雅降级）。

现有两个独立 Python 分析脚本（`cost_analysis.py` 541 行、`warehouse_type_analysis.py` 548 行）包含已验证的核心算法：
- 日粒度 × 仓库维度数据聚合（出库、入库、出勤、上架、退货）
- 月度费用基线估算（公式：总工时 × 加权均价 × 1.06）
- Pearson 相关性分析（10 个因素 vs 日工时）
- 仓库类型划分（仓位/库存/区域分布分析）

Python 服务需要对接的约束：
- MySQL 5.7 只读直连（10.126.50.199:3306, `wh_op_baseline`）
- SQLite 轻量存储预计算结果（Docker volume 持久化）
- 响应格式需匹配 `FactorRankVO`（rank, factorName, correlation, description）和 `CorrelationMatrixVO`（factors, matrix）

## Goals / Non-Goals

**Goals:**

- 搭建可独立运行的 FastAPI 微服务（`analytics/` 目录）
- 将现有脚本的数据聚合和分析逻辑重构为模块化服务
- 预计算模式：启动时全量计算、定时增量更新，结果写入 SQLite
- 提供 REST API 供 Spring Boot 后端调用
- Docker 化部署，通过环境变量注入配置

**Non-Goals:**

- 不实现实时计算（所有分析结果均为预计算）
- 不引入 pandas / numpy / scikit-learn 等重型依赖（纯 Python 实现 Pearson）
- 不实现认证/权限
- 不修改 Spring Boot 后端代码
- 不实现复杂的分布式任务调度（使用 APScheduler 单机调度）

## Decisions

### D1: 框架选择 — FastAPI + Uvicorn

**选择**: FastAPI (Pydantic v2) + Uvicorn ASGI server  
**替代方案**: Flask、Django REST Framework  
**原因**: FastAPI 原生支持 async、自动生成 OpenAPI 文档、Pydantic 数据校验内置；项目设计中已明确使用 FastAPI。

### D2: 数据库连接 — pymysql 连接池

**选择**: pymysql + 自定义连接池（上下文管理器模式）  
**替代方案**: SQLAlchemy ORM、aiomysql  
**原因**: 
- 现有脚本已使用 pymysql，迁移成本最低
- MySQL 查询均为只读聚合，ORM 层无必要
- 连接池使用 `queue.Queue` 实现简单池化，避免频繁建连
- 不使用 async MySQL 驱动，因为预计算任务在后台线程运行，同步查询更简单

### D3: 结果存储 — SQLite 三表结构

**选择**: SQLite 3 文件数据库，三张结果表  
**表结构**:
- `daily_metrics` — 日维度聚合指标（日期、仓库、出库/入库/出勤/上架/退货各维度指标）
- `baseline_results` — 月度费用基线（月份、仓库、总工时、均价、估算费用、单均/件均成本）
- `impact_results` — 影响因素分析结果（仓库、因素名、相关系数、排名、描述、矩阵 JSON）

**替代方案**: Redis 缓存、PostgreSQL  
**原因**: Docker 容器内轻量化存储，volume 挂载即可持久化，无额外基础设施依赖。

### D4: 预计算策略 — APScheduler

**选择**: APScheduler `BackgroundScheduler`，启动时全量 + 每 6 小时定时触发  
**替代方案**: Celery + Redis、asyncio 定时器  
**原因**: 
- 单机部署，无需分布式任务队列
- APScheduler 轻量，可嵌入 FastAPI 生命周期事件
- 预计算任务流：`daily_metrics` → `baseline_results` → `impact_results`（串行依赖）

### D5: API 设计 — 与 AnalyticsClient 契约对齐

**选择**: 直接返回与 `FactorRankVO` / `CorrelationMatrixVO` 结构匹配的 JSON  
**端点**:
- `GET /api/impact/factors?warehouseCode={code}` — 从 SQLite 读取预计算结果，返回 `[{rank, factorName, correlation, description}]`
- `GET /api/impact/correlation?warehouseCode={code}` — 从 SQLite 读取，返回 `{factors, matrix}`
- `GET /api/health` — 返回服务状态、MySQL/SQLite 连通性、最近预计算时间
- `GET /api/baseline/daily-metrics?warehouseCode={code}&month={yyyy-MM}` — 日维度明细（预留，供后续前端直接调用）
- `GET /api/baseline/monthly?warehouseCode={code}` — 月度基线汇总（预留）

### D6: 目录结构

```
analytics/
├── pyproject.toml
├── requirements.txt
├── Dockerfile
└── src/
    ├── main.py                  # FastAPI 入口 + 生命周期
    ├── config.py                # 环境变量配置
    ├── db/
    │   ├── mysql_client.py      # MySQL 连接池 (只读)
    │   └── sqlite_client.py     # SQLite 读写
    ├── routers/
    │   ├── health.py            # GET /api/health
    │   ├── impact.py            # GET /api/impact/factors, /correlation
    │   └── baseline.py          # GET /api/baseline/daily-metrics, /monthly
    ├── services/
    │   ├── daily_metrics.py     # 日维度聚合 (MySQL → daily_metrics)
    │   ├── baseline_service.py  # 月度费用基线 (daily_metrics → baseline_results)
    │   ├── impact_service.py    # 影响因素分析 (daily_metrics → impact_results)
    │   └── correlation.py       # Pearson 相关系数计算
    ├── models/
    │   └── schemas.py           # Pydantic 模型
    └── tasks/
        └── scheduler.py         # APScheduler 预计算调度
```

## Risks / Trade-offs

| 风险 | 严重度 | 缓解措施 |
|---|---|---|
| **MySQL 大表查询慢**：仓位信息表 206 万行、工作量明细表 212 万行，聚合查询可能超时 | 高 | 预计算模式避免实时查询；SQL 使用 `DATE_FORMAT` + `GROUP BY` 减少扫描行数；后续可加索引 |
| **SQLite 并发写入**：预计算写入和 API 读取并发可能导致 "database is locked" | 中 | 使用 `WAL` 模式（Write-Ahead Logging）允许并发读；写入操作集中在预计算线程中串行执行 |
| **数据一致性**：预计算结果与实时 MySQL 数据存在时间差 | 低 | 预计算结果记录 `computed_at` 时间戳；API 响应包含数据更新时间；6 小时刷新周期可接受（原始数据也非实时更新） |
| **仓库名称映射**：`出勤统计表` 只有 `库房`（名称），无编码；部分表名称不完全一致（如 "常熟高新正创仓" vs "常熟高新正创B仓"） | 中 | 维护 `HOUSE_MAP` 和 `HOUSE_NAME_TO_ID` 双向映射字典；使用模糊匹配作为 fallback |
| **Python 服务宕机**：后端 AnalyticsClient 调用失败 | 低 | AnalyticsClient 已实现优雅降级（返回空数据）；health 端点用于 Docker 健康检查 |
