## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: Dashboard 概览 KPI 接口

系统 SHALL 提供 `GET /api/dashboard/overview` 接口，返回指定仓库和月份的核心 KPI 概览数据。

**Controller 层**: `DashboardController.overview(String warehouseCode, String month)` → `Result<DashboardOverviewVO>`

**Service 层**: `DashboardService.getOverview(warehouseCode, month)`
- 从出库单表（`outbound_order`）按仓库编码 + 月份聚合统计出库单量和出库件数
- 从出勤统计表（`attendance_statistics`）按仓库编码 + 月份聚合统计总工时和日均出勤人数
- 从报价信息表（`quotation_info`）获取该仓库的劳务单价
- 使用公式计算月度费用：`monthlyFee = totalWorkHours × weightedUnitPrice × (1 + 0.06)`
- 计算人效：`laborEfficiency = totalOrders / (avgHeadcount × workDays)`
- 计算单均成本：`costPerOrder = monthlyFee / totalOrders`
- 计算件均成本：`costPerItem = monthlyFee / totalItems`

**Mapper 层**: 使用 `QueryWrapper` + `selectMaps` 对出库单表、出勤统计表进行 GROUP BY 聚合查询

**请求示例**:
```
GET /api/dashboard/overview?warehouseCode=12000004&month=2025-03
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "warehouseCode": "12000004",
    "warehouseName": "天津武清佩森A仓",
    "month": "2025-03",
    "totalOrders": 15230,
    "totalItems": 64727,
    "totalWorkHours": 3456.5,
    "monthlyFee": 74680.23,
    "laborEfficiency": 58.2,
    "avgCostPerOrder": 4.90,
    "avgCostPerItem": 1.15,
    "avgHeadcount": 12
  }
}
```

#### Scenario: 正常查询 Dashboard 概览

- **WHEN** 客户端请求 `GET /api/dashboard/overview?warehouseCode=12000004&month=2025-03`
- **THEN** 系统返回 `code=200`，`data` 中包含 `totalOrders`、`totalItems`、`totalWorkHours`、`monthlyFee`、`laborEfficiency`、`avgCostPerOrder`、`avgCostPerItem`、`avgHeadcount` 字段，所有数值均由 MySQL 聚合计算得出

#### Scenario: 缺少必要参数

- **WHEN** 客户端请求 `GET /api/dashboard/overview`（未提供 warehouseCode 或 month）
- **THEN** 系统返回 `code=200`，`data` 中展示所有仓库汇总的 KPI

### Requirement: Dashboard 趋势数据接口

系统 SHALL 提供 `GET /api/dashboard/trend` 接口，返回指定仓库在时间范围内的趋势数据。

**Controller 层**: `DashboardController.trend(String warehouseCode, String startMonth, String endMonth, String type)` → `Result<List<TrendDataVO>>`

**Service 层**: `DashboardService.getTrend(warehouseCode, startMonth, endMonth, type)`
- `type` 支持以下值：
  - `outbound_orders` — 日出库单量趋势（从出库单表按日聚合）
  - `fee` — 月度费用趋势（按月聚合计算）
  - `workload_distribution` — 工作量分布（从工作量统计表按操作类型聚合）
- 根据 `type` 从对应表中按日期维度聚合数据

**Mapper 层**: 按日期 GROUP BY 聚合，WHERE 条件包含仓库编码 + 时间范围

**请求示例**:
```
GET /api/dashboard/trend?warehouseCode=12000004&startMonth=2025-03&endMonth=2025-08&type=outbound_orders
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "date": "2025-03-01",
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓",
      "value": 523.0,
      "type": "outbound_orders"
    },
    {
      "date": "2025-03-02",
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓",
      "value": 498.0,
      "type": "outbound_orders"
    }
  ]
}
```

#### Scenario: 查询日出库单量趋势

- **WHEN** 客户端请求 `GET /api/dashboard/trend?warehouseCode=12000004&startMonth=2025-03&endMonth=2025-03&type=outbound_orders`
- **THEN** 系统返回该仓库 2025-03 每天的出库单量列表，每条记录包含 `date`、`warehouseCode`、`warehouseName`、`value`、`type` 字段

#### Scenario: 查询月度费用趋势

- **WHEN** 客户端请求 `GET /api/dashboard/trend?warehouseCode=12000004&startMonth=2025-03&endMonth=2025-08&type=fee`
- **THEN** 系统返回该仓库每月的操作费用列表

#### Scenario: 未指定仓库编码

- **WHEN** 客户端请求 `GET /api/dashboard/trend?startMonth=2025-03&endMonth=2025-08&type=outbound_orders`（未提供 warehouseCode）
- **THEN** 系统返回所有仓库合并的趋势数据
