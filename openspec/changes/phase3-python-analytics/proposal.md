## Why

后端 Spring Boot API（Phase 2）已通过 `AnalyticsClient` 代理调用 Python 分析服务的 `/api/impact/factors` 和 `/api/impact/correlation` 端点，但该服务尚不存在。同时，前端需要的影响因素分析、费用基线预计算等重计算任务不适合在 Java 侧实时完成，需要独立的 Python FastAPI 微服务承载数据分析算法，预计算结果写入 SQLite 供快速查询。现有 `cost_analysis.py`（541 行）和 `warehouse_type_analysis.py`（548 行）中已验证的分析逻辑需迁移为可服务化的模块。

## What Changes

- 创建 `analytics/` 目录下的 FastAPI 项目骨架（pyproject.toml、requirements.txt、Dockerfile）
- 实现 MySQL 连接模块（pymysql，直连 10.126.50.199:3306，只读）
- 实现 SQLite 结果存储模块（预计算结果持久化到 Docker volume）
- 实现健康检查 API（`GET /api/health`）
- 迁移 `cost_analysis.py` 核心逻辑为费用基线分析服务（日粒度 × 仓库维度汇总、月度费用基线计算、结果写入 SQLite `baseline_results` 表）
- 迁移 Pearson 相关性分析逻辑为影响因素分析服务（相关系数计算、因素重要性排序、结果写入 SQLite `impact_results` 表）
- 实现日维度明细计算模块（聚合出库/入库/退货/出勤/上架等指标，结果写入 SQLite `daily_metrics` 表）
- 实现预计算调度器（启动时全量计算 + 定时增量触发）
- 实现 REST API 端点供 Spring Boot AnalyticsClient 调用（`/api/impact/factors`、`/api/impact/correlation`）

### 非目标

- 不涉及前端 Vue 代码
- 不修改后端 Spring Boot 代码（AnalyticsClient 调用约定已在 Phase 2 确定）
- 不引入 OLAP 引擎或复杂的机器学习模型（第一版 MVP）
- 不实现认证/权限控制

## Capabilities

### New Capabilities

- `fastapi-project-skeleton`: FastAPI 项目骨架搭建，包括 pyproject.toml、requirements.txt、目录结构、入口文件、配置模块
- `mysql-reader`: Python 侧 MySQL 只读连接模块，pymysql 连接池，环境变量配置
- `sqlite-storage`: SQLite 结果存储模块，表结构定义（baseline_results、impact_results、daily_metrics），读写接口
- `health-api`: 健康检查端点 `/api/health`，返回服务状态、数据库连通性、最近预计算时间
- `baseline-analysis`: 费用基线分析服务，日维度×仓库聚合、月度费用基线计算、结果持久化，迁移自 cost_analysis.py
- `impact-analysis`: 影响因素分析服务，Pearson 相关系数、因素重要性排序、相关性矩阵，供 AnalyticsClient 调用
- `daily-metrics`: 日维度明细计算模块，聚合出库/入库/退货/出勤/上架/工作量等多源指标
- `precompute-scheduler`: 预计算调度器，启动时全量计算 + 定时触发，任务状态管理
- `analytics-dockerfile`: Python FastAPI 服务 Dockerfile，多阶段构建

### Modified Capabilities

- `analytics-client`: 明确 Python 服务端的响应格式需与 AnalyticsClient 期望的 FactorRankVO / CorrelationMatrixVO 结构一致

## Impact

- **新增目录**: `wh-op-platform/analytics/` 整个 Python FastAPI 项目
- **新增文件**: SQLite 数据库文件 `data/results.db`（Docker volume 挂载，Git 忽略）
- **依赖**: pymysql、fastapi、uvicorn、pydantic、apscheduler
- **端口**: 8000（FastAPI 服务）
- **环境变量**: `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE`、`SQLITE_PATH`
- **API 约定**: `/api/impact/factors?warehouseCode={code}` 返回 `List<FactorRankVO>` 格式，`/api/impact/correlation?warehouseCode={code}` 返回 `CorrelationMatrixVO` 格式
- **已有脚本**: `cost_analysis.py` 和 `warehouse_type_analysis.py` 的核心逻辑将被重构迁移，原脚本保留但不再维护
