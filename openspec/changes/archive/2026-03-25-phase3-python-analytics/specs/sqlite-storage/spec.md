## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: SQLite 结果存储模块

系统 SHALL 提供 SQLite 存储模块 `db/sqlite_client.py`，管理三张预计算结果表的创建、写入和查询。

**数据库文件**: 路径由 `config.settings.SQLITE_PATH` 决定（默认 `./data/results.db`），Docker 部署时挂载到 volume

**WAL 模式**: 数据库 MUST 启用 `PRAGMA journal_mode=WAL` 以支持并发读写

**表结构**:

**daily_metrics 表**:
```sql
CREATE TABLE IF NOT EXISTS daily_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,              -- yyyy-MM-dd
    warehouse_code TEXT NOT NULL,
    warehouse_name TEXT,
    ob_orders INTEGER DEFAULT 0,     -- 出库单量
    ob_items REAL DEFAULT 0,         -- 出库件数
    item_order_ratio REAL DEFAULT 0, -- 件单比
    ib_orders INTEGER DEFAULT 0,     -- 入库单量
    ib_items REAL DEFAULT 0,         -- 入库件数
    return_orders INTEGER DEFAULT 0, -- 退货量
    shelf_orders INTEGER DEFAULT 0,  -- 上架量
    shelf_items REAL DEFAULT 0,      -- 上架件数
    headcount INTEGER DEFAULT 0,     -- 出勤人数
    total_work_hours REAL DEFAULT 0, -- 总工时 (小时)
    fixed_count INTEGER DEFAULT 0,   -- 固定劳务人数
    temp_count INTEGER DEFAULT 0,    -- 临时劳务人数
    own_count INTEGER DEFAULT 0,     -- 自有人员人数
    fixed_temp_ratio REAL DEFAULT 0, -- 固临比
    computed_at TEXT NOT NULL,        -- 计算时间
    UNIQUE(date, warehouse_code)
);
```

**baseline_results 表**:
```sql
CREATE TABLE IF NOT EXISTS baseline_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    month TEXT NOT NULL,              -- yyyy-MM
    warehouse_code TEXT NOT NULL,
    warehouse_name TEXT,
    total_orders INTEGER DEFAULT 0,
    total_items REAL DEFAULT 0,
    total_work_hours REAL DEFAULT 0,
    avg_unit_price REAL DEFAULT 0,   -- 加权平均单价 (元/h)
    estimated_fee REAL DEFAULT 0,    -- 估算月度费用 (元)
    cost_per_order REAL DEFAULT 0,   -- 单均成本
    cost_per_item REAL DEFAULT 0,    -- 件均成本
    avg_headcount INTEGER DEFAULT 0,
    working_days INTEGER DEFAULT 0,
    computed_at TEXT NOT NULL,
    UNIQUE(month, warehouse_code)
);
```

**impact_results 表**:
```sql
CREATE TABLE IF NOT EXISTS impact_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_code TEXT NOT NULL,
    factor_name TEXT NOT NULL,
    correlation REAL DEFAULT 0,      -- Pearson 相关系数
    rank INTEGER DEFAULT 0,
    description TEXT,
    matrix_json TEXT,                -- 完整相关性矩阵 (JSON, 仅 rank=1 的行存储)
    sample_count INTEGER DEFAULT 0,
    computed_at TEXT NOT NULL,
    UNIQUE(warehouse_code, factor_name)
);
```

**接口方法**:
- `init_db()`: 创建数据库文件和三张表（IF NOT EXISTS），设置 WAL 模式
- `upsert_daily_metrics(records: list[dict])`: 批量写入/更新日维度指标
- `upsert_baseline_results(records: list[dict])`: 批量写入/更新月度基线
- `upsert_impact_results(records: list[dict])`: 批量写入/更新影响因素结果
- `query_daily_metrics(warehouse_code, month)`: 查询日维度数据
- `query_baseline_results(warehouse_code)`: 查询月度基线
- `query_impact_factors(warehouse_code)`: 查询影响因素排序
- `query_correlation_matrix(warehouse_code)`: 查询相关性矩阵
- `get_last_computed_at()`: 获取最近预计算时间

#### Scenario: 首次启动时自动创建表

- **WHEN** 调用 `init_db()` 且数据库文件不存在
- **THEN** 创建 `results.db` 文件，包含 daily_metrics、baseline_results、impact_results 三张空表，WAL 模式已启用

#### Scenario: 批量写入日维度指标

- **WHEN** 调用 `upsert_daily_metrics([{date: "2025-03-01", warehouse_code: "12000004", ob_orders: 150, ...}])`
- **THEN** 数据写入 daily_metrics 表，若同一 (date, warehouse_code) 已存在则更新

#### Scenario: 查询影响因素结果

- **WHEN** 调用 `query_impact_factors("12000004")`
- **THEN** 返回该仓库所有影响因素的列表，按 rank 升序排列，包含 factor_name、correlation、rank、description 字段
