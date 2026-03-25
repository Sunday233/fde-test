# mysql-query-optimization

**所属服务**: backend（MySQL 索引）+ analytics（MySQL 查询）

## ADDED Requirements

### Requirement: 出库单表复合索引

系统 SHALL 在 MySQL `出库单表` 上创建复合索引 `idx_outbound_wh_time (库房编码, 创建时间)`，覆盖 Dashboard 和 Baseline 服务中按仓库 + 时间范围的聚合查询。

#### Scenario: 出库单按仓库+月份聚合查询使用索引

- **WHEN** 执行 `SELECT COUNT(*) FROM 出库单表 WHERE 库房编码 = 'WH001' AND 创建时间 BETWEEN '2025-03-01' AND '2025-03-31'`
- **THEN** `EXPLAIN` 显示使用 `idx_outbound_wh_time` 索引，type 为 `range` 或 `ref`，而非 `ALL`

### Requirement: 出勤统计表复合索引

系统 SHALL 在 `出勤统计表` 上创建复合索引 `idx_attendance_wh_date (库房, 考勤日期)`，覆盖按仓库 + 日期范围的工时汇总查询。

#### Scenario: 出勤统计按仓库+月份查询使用索引

- **WHEN** 执行按 `库房` 和 `考勤日期` 范围的 SUM/COUNT 聚合查询
- **THEN** `EXPLAIN` 显示使用 `idx_attendance_wh_date` 索引

### Requirement: 报价信息表复合索引

系统 SHALL 在 `报价信息表` 上创建复合索引 `idx_quote_wh_status (库房名称, 报价状态)`，覆盖费用基线计算中的单价查询。

#### Scenario: 报价查询使用索引

- **WHEN** 执行 `SELECT AVG(单价) FROM 报价信息表 WHERE 库房名称 = 'xxx' AND 报价状态 = '正常'`
- **THEN** 查询使用 `idx_quote_wh_status` 索引

### Requirement: 工作量统计信息表复合索引

系统 SHALL 在 `工作量统计信息表` 上创建复合索引 `idx_workload_wh_month (库房编码, 月份)`。

#### Scenario: 工作量统计按仓库+月份查询使用索引

- **WHEN** 执行按 `库房编码` 和 `月份` 筛选的工作量查询
- **THEN** 查询使用 `idx_workload_wh_month` 索引

### Requirement: 入库单表复合索引

系统 SHALL 在 `入库单表` 上创建复合索引 `idx_inbound_wh_time (库房编码, 创建时间)`。

#### Scenario: 入库单按仓库+时间聚合查询使用索引

- **WHEN** 执行按 `库房编码` 和 `创建时间` 范围的入库统计查询
- **THEN** 查询使用 `idx_inbound_wh_time` 索引

### Requirement: 索引创建脚本

系统 SHALL 提供 SQL 迁移脚本 `wh-op-platform/backend/src/main/resources/db/migration/V1__add_indexes.sql`，包含所有索引的 `CREATE INDEX IF NOT EXISTS` 语句。索引使用 `IF NOT EXISTS` 以支持幂等执行。

#### Scenario: 脚本幂等执行

- **WHEN** 同一脚本执行两次
- **THEN** 第二次执行不报错，索引已存在则跳过

### Requirement: Baseline 列表后端分页

`GET /api/baseline/monthly` SHALL 支持分页参数 `page`（默认 1）和 `size`（默认 20）。返回结构 SHALL 包含 `records[]`、`total`、`page`、`size` 字段。不传分页参数时保持向后兼容，返回全量数据。

#### Scenario: 带分页参数返回分页结果

- **WHEN** 请求 `GET /api/baseline/monthly?page=1&size=20`
- **THEN** 响应 JSON 包含 `records` 数组（≤20 条）、`total`（总记录数）、`page: 1`、`size: 20`

#### Scenario: 不带分页参数向后兼容

- **WHEN** 请求 `GET /api/baseline/monthly`（无 page/size）
- **THEN** 返回全量数据（与当前行为一致）

### Requirement: Report 列表后端分页

`GET /api/report/list` SHALL 支持分页参数 `page`（默认 1）和 `size`（默认 20），返回结构同 Baseline 分页。

#### Scenario: 报告列表分页

- **WHEN** 请求 `GET /api/report/list?page=1&size=10`
- **THEN** 响应包含不超过 10 条报告记录和 `total` 总数
