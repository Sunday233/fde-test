## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: 预计算调度器

系统 SHALL 提供预计算调度器 `tasks/scheduler.py`，管理数据分析任务的定时执行。

**调度引擎**: APScheduler `BackgroundScheduler`

**任务流**（串行执行，有依赖关系）:
1. `daily_metrics` 计算 — 从 MySQL 聚合日维度指标到 SQLite
2. `baseline_results` 计算 — 从 daily_metrics 聚合月度费用基线
3. `impact_results` 计算 — 从 daily_metrics 计算 Pearson 相关性

**触发策略**:
- **启动时全量计算**: FastAPI lifespan 启动事件中触发一次全量预计算
- **定时增量**: 每隔 N 小时触发（N 由 `config.settings.PRECOMPUTE_INTERVAL_HOURS` 控制，默认 6）
- 使用 APScheduler `IntervalTrigger`

**任务管理**:
- 任务执行期间 MUST 记录日志：开始时间、结束时间、处理记录数
- 任务失败 MUST 记录异常信息但不终止服务
- 并发保护：同一时刻只允许一个预计算任务运行（使用 `max_instances=1`）

**生命周期集成**:
```python
# main.py lifespan
@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    init_db()
    start_scheduler()
    run_precompute()  # 首次全量
    yield
    # shutdown
    stop_scheduler()
```

#### Scenario: 启动时触发首次预计算

- **WHEN** FastAPI 服务启动完成
- **THEN** 自动执行一次全量预计算（daily_metrics → baseline_results → impact_results），日志输出各步骤耗时

#### Scenario: 定时触发预计算

- **WHEN** 距上次预计算已过 6 小时
- **THEN** APScheduler 触发新一轮预计算，按 daily_metrics → baseline → impact 顺序执行

#### Scenario: 预计算期间 MySQL 不可达

- **WHEN** 预计算任务执行中 MySQL 连接失败
- **THEN** 记录 ERROR 级别日志，任务中止但服务继续运行，下次定时触发时重试

#### Scenario: 防止并发重入

- **WHEN** 上一次预计算尚未完成，定时触发器再次触发
- **THEN** 新触发被跳过（`max_instances=1`），日志记录 "Precompute already running, skipping"
