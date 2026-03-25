## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 成本估算计算接口

系统 SHALL 提供 `POST /api/estimate/calculate` 接口，根据用户输入参数计算预估费用。

**Controller 层**: `EstimateController.calculate(EstimateRequest request)` → `Result<EstimateResultVO>`
- 请求体 MUST 使用 `@Valid` 进行参数校验

**Service 层**: `EstimateService.calculate(EstimateRequest request)`
- 计算逻辑（公式硬编码）：
  ```
  dailyItems = dailyOrders × itemsPerOrder
  estimatedHeadcount = ceil(dailyOrders / laborEfficiency)
  dailyHours = estimatedHeadcount × 8
  estimatedTotalHours = dailyHours × workDays
  weightedUnitPrice = fixedLaborRatio × fixedLaborPrice + (1 - fixedLaborRatio) × tempLaborPrice
  monthlyFee = estimatedTotalHours × weightedUnitPrice × (1 + taxRate)
  costPerOrder = monthlyFee / (dailyOrders × workDays)
  costPerItem = monthlyFee / (dailyItems × workDays)
  ```

**Mapper 层**: 无（纯计算，不查询数据库）

**请求示例**:
```
POST /api/estimate/calculate
Content-Type: application/json

{
  "dailyOrders": 500,
  "itemsPerOrder": 4.25,
  "workDays": 26,
  "laborEfficiency": 50,
  "fixedLaborPrice": 22.0,
  "tempLaborPrice": 18.0,
  "fixedLaborRatio": 0.67,
  "taxRate": 0.06
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "estimatedHeadcount": 10,
    "estimatedTotalHours": 2080.0,
    "weightedUnitPrice": 20.68,
    "monthlyFee": 45595.42,
    "costPerOrder": 3.51,
    "costPerItem": 0.83
  }
}
```

#### Scenario: 正常计算成本估算

- **WHEN** 客户端提交 `POST /api/estimate/calculate`，请求体包含 `dailyOrders=500`、`itemsPerOrder=4.25`、`workDays=26`、`laborEfficiency=50`、`fixedLaborPrice=22.0`、`tempLaborPrice=18.0`、`fixedLaborRatio=0.67`、`taxRate=0.06`
- **THEN** 系统返回 `code=200`，`data` 中 `estimatedHeadcount=10`（ceil(500/50)），`estimatedTotalHours=2080`（10×8×26），`weightedUnitPrice=20.68`（0.67×22+0.33×18），`monthlyFee`= estimatedTotalHours × weightedUnitPrice × 1.06

#### Scenario: 缺少必要参数

- **WHEN** 客户端提交 `POST /api/estimate/calculate` 时缺少 `dailyOrders` 字段
- **THEN** 系统返回 `code=500`，`message` 包含参数校验错误信息

#### Scenario: 使用默认税率

- **WHEN** 客户端提交请求体中未包含 `taxRate` 字段
- **THEN** 系统使用默认税率 0.06 进行计算

### Requirement: 历史默认参数接口

系统 SHALL 提供 `GET /api/estimate/defaults/{warehouseCode}` 接口，返回该仓库的历史平均值作为估算默认参数。

**Controller 层**: `EstimateController.defaults(String warehouseCode)` → `Result<EstimateRequest>`

**Service 层**: `EstimateService.getDefaults(warehouseCode)`
- 从出库单表聚合：日均单量、件单比
- 从出勤统计表聚合：日均人数 → 推算人效
- 从工作量统计表聚合：月工作天数
- 从报价信息表获取：劳务单价和占比
- 组装默认参数返回

**Mapper 层**: 使用 `QueryWrapper` 聚合查询出库单表、出勤统计表、报价信息表

**请求示例**:
```
GET /api/estimate/defaults/12000004
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dailyOrders": 491.6,
    "itemsPerOrder": 4.25,
    "workDays": 26,
    "laborEfficiency": 58.2,
    "fixedLaborPrice": 22.0,
    "tempLaborPrice": 18.0,
    "fixedLaborRatio": 0.67,
    "taxRate": 0.06
  }
}
```

#### Scenario: 获取 A 仓默认参数

- **WHEN** 客户端请求 `GET /api/estimate/defaults/12000004`
- **THEN** 系统返回 `code=200`，`data` 包含天津A仓历史数据计算出的默认参数，各字段均为数值类型

#### Scenario: 仓库编码不存在

- **WHEN** 客户端请求 `GET /api/estimate/defaults/99999999`
- **THEN** 系统返回 `code=200`，`data` 包含系统级别的全局默认值（预设合理值）
