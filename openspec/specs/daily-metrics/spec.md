## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: 日维度明细计算模块

系统 SHALL 提供日维度明细计算服务 `services/daily_metrics.py`，从 MySQL 原始表聚合多源指标到 SQLite `daily_metrics` 表。

**算法来源**: 迁移自 `cost_analysis.py` 第 30–265 行的 7 个数据源日维度聚合逻辑

**数据流**: MySQL 19 张原始表 → SQL 聚合查询 → `daily_metrics` (SQLite)

**聚合维度**: 日期 (yyyy-MM-dd) × 仓库编码

**数据源 SQL（均为 MySQL 只读查询）**:

1. **出库日维度** (出库单表):
```sql
SELECT DATE_FORMAT(`创建时间`, '%Y-%m-%d') AS ymd,
       `库房编码` AS warehouse_code,
       COUNT(*) AS ob_orders,
       SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS ob_items
FROM `出库单表`
WHERE `状态` != '撤单' AND `创建时间` IS NOT NULL
GROUP BY ymd, warehouse_code
```

2. **入库日维度** (入库单表):
```sql
SELECT DATE_FORMAT(`创单时间`, '%Y-%m-%d') AS ymd,
       `库房编码` AS warehouse_code,
       COUNT(*) AS ib_orders,
       SUM(CAST(`物料总数量` AS DECIMAL(20,2))) AS ib_items
FROM `入库单表`
WHERE `状态` = '正常' AND `创单时间` IS NOT NULL AND `库房编码` IS NOT NULL
GROUP BY ymd, warehouse_code
```

3. **出勤日维度** (出勤统计表):
```sql
SELECT DATE_FORMAT(`考勤日期`, '%Y-%m-%d') AS ymd,
       `库房` AS warehouse_name,
       COUNT(DISTINCT `员工编码`) AS headcount,
       SUM(CAST(`工作时长` AS DECIMAL(10,2))) AS total_work_minutes,
       COUNT(DISTINCT CASE WHEN `员工类型` = '长期劳务' THEN `员工编码` END) AS fixed_count,
       COUNT(DISTINCT CASE WHEN `员工类型` = '临时劳务' THEN `员工编码` END) AS temp_count,
       COUNT(DISTINCT CASE WHEN `员工类型` = '自有人员' THEN `员工编码` END) AS own_count
FROM `出勤统计表`
GROUP BY ymd, `库房`
```
注意：出勤统计表只有 `库房`（名称），需通过 `HOUSE_NAME_TO_ID` 映射获取仓库编码。`工作时长`为分钟，需 ÷60 转换为小时。

4. **上架日维度** (上架单表):
```sql
SELECT DATE_FORMAT(`创建时间`, '%Y-%m-%d') AS ymd,
       `库房编码` AS warehouse_code,
       COUNT(*) AS shelf_orders,
       SUM(CAST(`上架单总数量` AS DECIMAL(20,2))) AS shelf_items
FROM `上架单表`
GROUP BY ymd, warehouse_code
```

5. **退货日维度** (退货信息表 JOIN 入库单表):
```sql
SELECT DATE_FORMAT(i.`创单时间`, '%Y-%m-%d') AS ymd,
       i.`库房编码` AS warehouse_code,
       COUNT(*) AS return_orders
FROM `退货信息表` r
JOIN `入库单表` i ON r.`入库单号` = i.`入库单号`
WHERE i.`状态` = '正常' AND i.`创单时间` IS NOT NULL AND i.`库房编码` IS NOT NULL
GROUP BY ymd, warehouse_code
```

**合并逻辑**:
- 以 (ymd, warehouse_code) 为 key，合并 5 个数据源
- 计算派生指标：`item_order_ratio = ob_items / ob_orders`（ob_orders > 0 时）、`fixed_temp_ratio = fixed_count / temp_count`（temp_count > 0 时）
- `total_work_hours = total_work_minutes / 60`
- 通过 `HOUSE_MAP` 填充 `warehouse_name`

**仓库映射**:
```python
HOUSE_MAP = {'12000004': '天津武清佩森A仓', '32050005': '常熟高新正创B仓'}
HOUSE_NAME_TO_ID = {'天津武清佩森A仓': '12000004', '常熟高新正创B仓': '32050005', '常熟高新正创仓': '32050005'}
```

#### Scenario: 全量计算日维度指标

- **WHEN** 预计算调度器触发全量计算
- **THEN** 从 MySQL 查询 5 个数据源，按 (date, warehouse_code) 合并，写入 SQLite daily_metrics 表，每条记录包含 computed_at 时间戳

#### Scenario: 出勤表仓库名称映射

- **WHEN** 出勤统计表返回 `库房`="天津武清佩森A仓"
- **THEN** 通过 HOUSE_NAME_TO_ID 映射为 warehouse_code="12000004"

#### Scenario: 数据源缺失时补零

- **WHEN** 某日某仓库有出库数据但无退货数据
- **THEN** 该日该仓库的 return_orders 字段为 0，其他已有字段正常写入
