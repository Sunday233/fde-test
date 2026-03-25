## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: 费用基线分析服务

系统 SHALL 提供费用基线分析服务 `services/baseline_service.py`，从 daily_metrics 聚合月度费用基线，并对外提供 API 查询。

**算法来源**: 迁移自 `cost_analysis.py` 第 164–384 行的月度汇总与费用估算逻辑

**数据流**: `daily_metrics` (SQLite) → 月度聚合 → `baseline_results` (SQLite)

**月度基线计算逻辑**:
1. 按 `(month, warehouse_code)` 分组聚合 daily_metrics：
   - `total_orders` = SUM(ob_orders)
   - `total_items` = SUM(ob_items)
   - `total_work_hours` = SUM(total_work_hours)
   - `avg_headcount` = ROUND(AVG(headcount))
   - `working_days` = COUNT(DISTINCT date WHERE ob_orders > 0)
2. 从 MySQL `报价信息表` 获取加权平均单价：
   ```sql
   SELECT `库房名称`, AVG(`供应商结算单价`) AS avg_price
   FROM `报价信息表` WHERE `报价状态` = '正常'
   GROUP BY `库房名称`
   ```
3. 费用计算公式：
   $$\text{estimated\_fee} = \text{total\_work\_hours} \times \text{avg\_unit\_price} \times 1.06$$
4. 单均/件均成本：
   - `cost_per_order` = estimated_fee / total_orders
   - `cost_per_item` = estimated_fee / total_items

**仓库名称映射**: `HOUSE_MAP = {'12000004': '天津武清佩森A仓', '32050005': '常熟高新正创B仓'}`，报价信息表中 "常熟高新正创仓" 需映射到 "32050005"

**API 端点**:

`GET /api/baseline/monthly`  
查询参数: `warehouseCode`（可选，不传则返回所有仓库）  
响应:
```json
[
  {
    "month": "2025-03",
    "warehouseCode": "12000004",
    "warehouseName": "天津武清佩森A仓",
    "totalOrders": 12500,
    "totalItems": 45000.0,
    "totalWorkHours": 8500.0,
    "avgUnitPrice": 25.67,
    "estimatedFee": 231234.0,
    "costPerOrder": 18.50,
    "costPerItem": 5.14,
    "avgHeadcount": 35,
    "workingDays": 26,
    "computedAt": "2025-03-25T10:00:00"
  }
]
```

#### Scenario: 计算月度费用基线

- **WHEN** 预计算触发，daily_metrics 中有 2025-03 的天津仓数据
- **THEN** baseline_results 写入一条记录：month=2025-03, warehouse_code=12000004, estimated_fee = total_work_hours × avg_unit_price × 1.06

#### Scenario: 查询所有仓库月度基线

- **WHEN** 调用 `GET /api/baseline/monthly`（不传 warehouseCode）
- **THEN** 返回所有仓库所有月份的基线数据，按 month DESC, warehouse_code 排序

#### Scenario: 查询指定仓库月度基线

- **WHEN** 调用 `GET /api/baseline/monthly?warehouseCode=12000004`
- **THEN** 仅返回天津武清佩森A仓的月度基线数据
