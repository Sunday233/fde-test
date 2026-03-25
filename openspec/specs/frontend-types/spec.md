# frontend-types

**所属服务**: frontend

## ADDED Requirements

### Requirement: Result 包装类型

系统 SHALL 在 `src/types/api.ts` 中定义后端统一响应包装类型：

```ts
interface Result<T> {
  code: number       // 200=成功, 500=错误
  message: string
  data: T
}
```

此类型用于响应拦截器内部，API 方法返回值为解包后的 `T` 类型。

#### Scenario: Result 类型用于拦截器

- **WHEN** 响应拦截器接收后端 JSON 响应
- **THEN** 将响应体断言为 `Result<unknown>` 进行 code 判断和 data 提取

### Requirement: Dashboard 相关类型

系统 SHALL 定义以下类型：

```ts
interface DashboardOverviewVO {
  totalOrders: number        // 总出库单量
  totalWorkHours: number     // 总工时（小时）
  monthlyFee: number         // 月度操作费用（元）
  laborEfficiency: number    // 人效（单/人/天）
  avgCostPerOrder: number    // 单均成本
  avgCostPerItem: number     // 件均成本
}

interface TrendDataVO {
  date: string               // 日期
  warehouseCode: string
  warehouseName: string
  value: number
  type: string               // outbound_orders / fee / workload_distribution
}
```

#### Scenario: DashboardOverviewVO 字段对齐后端

- **WHEN** 后端 `DashboardOverviewVO.java` 返回 JSON
- **THEN** 前端 `DashboardOverviewVO` 接口的字段名与 JSON key 一一对应

### Requirement: Baseline 相关类型

系统 SHALL 定义以下类型：

```ts
interface MonthlyBaselineVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number        // 日均费用
  totalFee: number           // 月度总费用
  totalOrders: number        // 总出库单量
  totalItems: number         // 总出库件数
  costPerOrder: number       // 单均成本
  costPerItem: number        // 件均成本
  avgHeadcount: number       // 日均出勤人数
  totalWorkHours: number     // 总工时
  weightedUnitPrice: number  // 加权平均劳务单价
}

interface WarehouseDetailVO {
  warehouseCode: string
  warehouseName: string
  year: number
  month: number
  dailyAvgFee: number
  totalFee: number
  totalOrders: number
  totalItems: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
  totalWorkHours: number
  weightedUnitPrice: number
}

interface CompareResultVO {
  warehouseCode: string
  warehouseName: string
  totalFee: number
  totalOrders: number
  costPerOrder: number
  costPerItem: number
  avgHeadcount: number
}
```

#### Scenario: MonthlyBaselineVO 数值字段类型正确

- **WHEN** 后端返回 `MonthlyBaselineVO` JSON，`dailyAvgFee` 为 `12345.67`
- **THEN** 前端 TypeScript 类型为 `number`，可直接用于数值计算和图表展示

### Requirement: Impact 相关类型

系统 SHALL 定义以下类型：

```ts
interface FactorRankVO {
  rank: number               // 排名
  factorName: string         // 因素名称
  correlation: number        // Pearson 相关系数
  description: string        // 描述
}

interface CorrelationMatrixVO {
  factors: string[]          // 因素名称列表
  matrix: number[][]         // 相关系数矩阵（对称方阵）
}
```

#### Scenario: CorrelationMatrixVO 矩阵结构

- **WHEN** 后端返回 `CorrelationMatrixVO`，`factors` 长度为 5
- **THEN** `matrix` 为 5×5 二维数组，`matrix[i][i]` 均为 1.0

### Requirement: Estimate 相关类型

系统 SHALL 定义以下类型：

```ts
interface EstimateRequest {
  dailyOrders: number         // 日均单量
  itemsPerOrder: number       // 件单比
  workDays: number            // 月工作天数
  laborEfficiency: number     // 人效（单/人/天）
  fixedLaborPrice: number     // 固定劳务单价
  tempLaborPrice: number      // 临时劳务单价
  fixedLaborRatio: number     // 固定劳务占比
  taxRate: number             // 税率（默认 0.06）
}

interface EstimateResultVO {
  estimatedHeadcount: number      // 预估人数
  estimatedTotalHours: number     // 预估月度总工时
  weightedUnitPrice: number       // 加权平均单价
  monthlyFee: number              // 月度操作费用
  costPerOrder: number            // 单均成本
  costPerItem: number             // 件均成本
}
```

#### Scenario: EstimateRequest 用于 POST 请求体

- **WHEN** 用户在成本估算页面填写表单并提交
- **THEN** 表单数据构造为 `EstimateRequest` 类型对象，序列化为 JSON 请求体

### Requirement: Report 相关类型

系统 SHALL 定义以下类型：

```ts
interface ReportGenerateRequest {
  warehouseCode: string
  startMonth: string          // 格式 "2025-03"
  endMonth: string            // 格式 "2025-08"
}

interface ReportVO {
  id: string                  // 报告唯一 ID
  title: string               // 报告标题
  warehouseCode: string
  warehouseName: string
  startMonth: string
  endMonth: string
  createdAt: string           // ISO 日期时间字符串
}
```

#### Scenario: ReportVO 日期字段格式

- **WHEN** 后端返回 `createdAt: "2025-03-26T10:30:00"`
- **THEN** 前端 `createdAt` 为 string 类型，显示层自行格式化

### Requirement: Warehouse 类型

系统 SHALL 定义以下类型：

```ts
interface WarehouseVO {
  warehouseCode: string       // 仓库编码
  warehouseName: string       // 仓库名称
}
```

#### Scenario: WarehouseVO 用于仓库选择器

- **WHEN** 调用 `getWarehouses()` 返回 `WarehouseVO[]`
- **THEN** 选择器使用 `warehouseCode` 作为 value，`warehouseName` 作为 label
