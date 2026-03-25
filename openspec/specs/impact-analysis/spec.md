## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: 影响因素分析服务

系统 SHALL 提供影响因素分析服务 `services/impact_service.py`，基于日维度数据计算 Pearson 相关系数，结果供 Spring Boot `AnalyticsClient` 调用。

**算法来源**: 迁移自 `cost_analysis.py` 第 220–456 行的 Pearson 相关性分析和因素排序逻辑

**数据流**: `daily_metrics` (SQLite) → Pearson 计算 → `impact_results` (SQLite)

**分析因素** (10 个，与现有脚本一致):
1. 出库单量 (ob_orders)
2. 出库件数 (ob_items)
3. 件单比 (item_order_ratio)
4. 入库单量 (ib_orders)
5. 退货量 (return_orders)
6. 出勤人数 (headcount)
7. 固定劳务人数 (fixed_count)
8. 临时劳务人数 (temp_count)
9. 固临比 (fixed_temp_ratio)
10. 上架量 (shelf_orders)

**目标变量**: 日度总工时 (total_work_hours)，作为操作费用的代理变量

**计算逻辑**:
1. 从 SQLite `daily_metrics` 查询指定仓库的所有日维度数据
2. 过滤有效样本：`total_work_hours > 0 AND ob_orders > 0`
3. 对每个因素计算与 total_work_hours 的 Pearson 相关系数 (使用 `services/correlation.py`)
4. 按 |r| 降序排名
5. 生成描述:
   - |r| > 0.7: "核心驱动因子"
   - 0.4 < |r| ≤ 0.7: "重要影响因子"
   - 0.2 < |r| ≤ 0.4: "次要影响因子"
   - |r| ≤ 0.2: "影响不显著"
6. 构建 10×10 相关性矩阵（所有因素之间的两两 Pearson 值）
7. 结果写入 `impact_results` 表

**API 端点**:

`GET /api/impact/factors?warehouseCode={code}`  
响应格式需匹配 Spring Boot `FactorRankVO`:
```json
[
  {"rank": 1, "factorName": "出勤人数", "correlation": 0.96, "description": "核心驱动因子"},
  {"rank": 2, "factorName": "临时劳务人数", "correlation": 0.87, "description": "核心驱动因子"}
]
```

`GET /api/impact/correlation?warehouseCode={code}`  
响应格式需匹配 Spring Boot `CorrelationMatrixVO`:
```json
{
  "factors": ["出库单量", "出库件数", "件单比", "入库单量", "退货量", "出勤人数", "固定劳务人数", "临时劳务人数", "固临比", "上架量"],
  "matrix": [[1.0, 0.95, ...], [0.95, 1.0, ...], ...]
}
```

#### Scenario: 计算影响因素排序

- **WHEN** 预计算触发，daily_metrics 中有天津仓 30 天有效数据
- **THEN** impact_results 写入 10 条记录（每个因素一条），rank 按 |correlation| 降序

#### Scenario: API 返回因素排序

- **WHEN** 调用 `GET /api/impact/factors?warehouseCode=12000004`
- **THEN** 返回按 rank 升序排列的因素列表，格式与 `FactorRankVO` 一致

#### Scenario: API 返回相关性矩阵

- **WHEN** 调用 `GET /api/impact/correlation?warehouseCode=12000004`
- **THEN** 返回 10×10 矩阵，对角线元素为 1.0，`factors` 包含 10 个因素名称

#### Scenario: 有效样本不足

- **WHEN** 某仓库的有效日维度数据少于 5 个样本
- **THEN** 返回空列表 `[]`（factors）或空矩阵 `{factors: [], matrix: []}`（correlation）
