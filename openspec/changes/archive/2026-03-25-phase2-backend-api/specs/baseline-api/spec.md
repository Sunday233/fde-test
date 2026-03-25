## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 月度费用基线汇总接口

系统 SHALL 提供 `GET /api/baseline/monthly` 接口，返回指定年月的各仓库月度费用基线汇总。

**Controller 层**: `BaselineController.monthly(String warehouseCode, Integer year, Integer month)` → `Result<List<MonthlyBaselineVO>>`

**Service 层**: `BaselineService.getMonthlyBaseline(warehouseCode, year, month)`
- 从出库单表按仓库编码 + 月份聚合：总出库单量、总出库件数
- 从出勤统计表按仓库编码 + 月份聚合：总工时、日均出勤人数
- 从报价信息表获取该仓库劳务单价
- 计算公式：
  - `weightedUnitPrice = fixedRatio × fixedPrice + (1 - fixedRatio) × tempPrice`
  - `totalFee = totalWorkHours × weightedUnitPrice × (1 + 0.06)`
  - `dailyAvgFee = totalFee / workDays`
  - `costPerOrder = totalFee / totalOrders`
  - `costPerItem = totalFee / totalItems`
- 若 warehouseCode 为空，返回所有仓库的基线列表

**Mapper 层**: 使用 `QueryWrapper` + `selectMaps` 进行 GROUP BY 聚合查询

**请求示例**:
```
GET /api/baseline/monthly?year=2025&month=3
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓",
      "year": 2025,
      "month": 3,
      "dailyAvgFee": 6349.20,
      "totalFee": 196426.20,
      "totalOrders": 15230,
      "totalItems": 64727,
      "costPerOrder": 12.90,
      "costPerItem": 3.03,
      "avgHeadcount": 12,
      "totalWorkHours": 3456.5,
      "weightedUnitPrice": 20.39
    },
    {
      "warehouseCode": "32050005",
      "warehouseName": "常熟高新正创B仓",
      "year": 2025,
      "month": 3,
      "dailyAvgFee": 6797.50,
      "totalFee": 210722.50,
      "totalOrders": 9800,
      "totalItems": 82908,
      "costPerOrder": 21.50,
      "costPerItem": 2.54,
      "avgHeadcount": 15,
      "totalWorkHours": 3890.0,
      "weightedUnitPrice": 23.96
    }
  ]
}
```

#### Scenario: 查询所有仓库月度基线

- **WHEN** 客户端请求 `GET /api/baseline/monthly?year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 为数组，每个元素包含一个仓库的月度费用基线数据，包括 `dailyAvgFee`、`totalFee`、`costPerOrder`、`costPerItem`、`totalWorkHours`、`weightedUnitPrice` 等字段

#### Scenario: 按仓库编码查询月度基线

- **WHEN** 客户端请求 `GET /api/baseline/monthly?warehouseCode=12000004&year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 数组中仅包含该仓库的基线数据

### Requirement: 仓库费用详情接口

系统 SHALL 提供 `GET /api/baseline/warehouse/{warehouseCode}` 接口，返回指定仓库的详细费用分析数据。

**Controller 层**: `BaselineController.warehouseDetail(String warehouseCode, Integer year, Integer month)` → `Result<WarehouseDetailVO>`

**Service 层**: `BaselineService.getWarehouseDetail(warehouseCode, year, month)`
- 在月度基线基础上，额外包含：
  - 各操作类型的工时占比（拣货、复核、上架、入库等）
  - 固定劳务 vs 临时劳务人员分布
  - 日均出库单量
  - 件单比（items per order）

**请求示例**:
```
GET /api/baseline/warehouse/12000004?year=2025&month=3
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "warehouseCode": "12000004",
    "warehouseName": "天津武清佩森A仓",
    "year": 2025,
    "month": 3,
    "totalFee": 196426.20,
    "dailyAvgFee": 6349.20,
    "totalOrders": 15230,
    "totalItems": 64727,
    "costPerOrder": 12.90,
    "costPerItem": 3.03,
    "itemsPerOrder": 4.25,
    "dailyAvgOrders": 491.6,
    "avgHeadcount": 12,
    "totalWorkHours": 3456.5,
    "weightedUnitPrice": 20.39,
    "workHoursBreakdown": {
      "picking": 1200.0,
      "verification": 800.0,
      "shelving": 600.0,
      "inbound": 500.0,
      "other": 356.5
    },
    "laborDistribution": {
      "fixedCount": 8,
      "tempCount": 4,
      "fixedRatio": 0.67
    }
  }
}
```

#### Scenario: 查询仓库费用详情

- **WHEN** 客户端请求 `GET /api/baseline/warehouse/12000004?year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 包含该仓库的完整费用分析，包括 `workHoursBreakdown`（各操作类型工时占比）和 `laborDistribution`（劳务人员分布）

#### Scenario: 仓库编码不存在

- **WHEN** 客户端请求 `GET /api/baseline/warehouse/99999999?year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 为 null 或空对象

### Requirement: 仓库费用对比接口

系统 SHALL 提供 `GET /api/baseline/compare` 接口，返回多个仓库在同一时间段的费用对比数据。

**Controller 层**: `BaselineController.compare(String codes, Integer year, Integer month)` → `Result<List<CompareResultVO>>`

**Service 层**: `BaselineService.compare(codes, year, month)`
- `codes` 为逗号分隔的仓库编码列表
- 对每个仓库分别查询月度基线，然后组装对比结果
- 对比维度：日均费用、单均成本、件均成本、人效、总工时

**请求示例**:
```
GET /api/baseline/compare?codes=12000004,32050005&year=2025&month=3
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓",
      "dailyAvgFee": 6349.20,
      "costPerOrder": 12.90,
      "costPerItem": 3.03,
      "laborEfficiency": 58.2,
      "totalWorkHours": 3456.5
    },
    {
      "warehouseCode": "32050005",
      "warehouseName": "常熟高新正创B仓",
      "dailyAvgFee": 6797.50,
      "costPerOrder": 21.50,
      "costPerItem": 2.54,
      "laborEfficiency": 45.6,
      "totalWorkHours": 3890.0
    }
  ]
}
```

#### Scenario: 双仓对比

- **WHEN** 客户端请求 `GET /api/baseline/compare?codes=12000004,32050005&year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 数组包含两个仓库的对比数据，每个元素包含 `dailyAvgFee`、`costPerOrder`、`costPerItem`、`laborEfficiency`、`totalWorkHours`

#### Scenario: 单仓对比（退化）

- **WHEN** 客户端请求 `GET /api/baseline/compare?codes=12000004&year=2025&month=3`
- **THEN** 系统返回 `code=200`，`data` 数组仅包含一个仓库的数据
