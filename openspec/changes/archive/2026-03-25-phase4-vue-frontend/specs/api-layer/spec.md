# api-layer

**所属服务**: frontend

## ADDED Requirements

### Requirement: Axios 实例配置

系统 SHALL 在 `src/api/index.ts` 中创建 Axios 实例，配置如下：

```ts
const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})
```

**响应拦截器** MUST 统一处理后端 `Result<T>` 包装结构：
- `code === 200` 时：返回 `data` 字段（自动解包）
- `code !== 200` 时：使用 Ant Design Vue 的 `message.error()` 显示 `result.message`，并 reject Promise
- HTTP 异常（网络错误、5xx）时：显示通用错误提示，reject Promise

数据流：
```
Vue 组件调用 api 方法
  → Axios 实例发送请求（baseURL: /api）
  → Nginx 反向代理到 backend:8080
  → 后端返回 Result<T> JSON
  → 响应拦截器解包 data 字段
  → 组件收到纯业务数据
```

#### Scenario: 正常响应自动解包

- **WHEN** 后端返回 `{"code": 200, "message": "success", "data": {...}}`
- **THEN** API 方法的 Promise resolve 值为 `data` 字段内容（如 `{...}`），不含外层 `code` 和 `message`

#### Scenario: 业务错误自动提示

- **WHEN** 后端返回 `{"code": 500, "message": "参数校验失败"}`
- **THEN** 页面显示 Ant Design Vue 的 `message.error("参数校验失败")`，Promise reject

#### Scenario: 网络错误处理

- **WHEN** 后端服务不可达，Axios 请求超时
- **THEN** 页面显示通用错误提示"网络错误，请稍后重试"，Promise reject

### Requirement: Dashboard API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 获取看板概览
export function getOverview(warehouseCode: string, month: string): Promise<DashboardOverviewVO>
// GET /api/dashboard/overview?warehouseCode=...&month=...

// 获取趋势数据
export function getTrend(warehouseCode: string, startMonth: string, endMonth: string, type: string): Promise<TrendDataVO[]>
// GET /api/dashboard/trend?warehouseCode=...&startMonth=...&endMonth=...&type=...
```

#### Scenario: 获取看板概览数据

- **WHEN** 调用 `getOverview('12000004', '2025-03')`
- **THEN** 发送 `GET /api/dashboard/overview?warehouseCode=12000004&month=2025-03`，返回 `DashboardOverviewVO` 对象

### Requirement: Baseline API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 获取月度基线列表
export function getMonthlyBaseline(warehouseCode?: string, year?: number, month?: number): Promise<MonthlyBaselineVO[]>
// GET /api/baseline/monthly?warehouseCode=...&year=...&month=...

// 获取仓库详情
export function getWarehouseDetail(warehouseCode: string, year?: number, month?: number): Promise<WarehouseDetailVO>
// GET /api/baseline/warehouse/{warehouseCode}?year=...&month=...

// 仓库对比
export function compareWarehouses(codes: string[], year?: number, month?: number): Promise<CompareResultVO[]>
// GET /api/baseline/compare?codes=A,B&year=...&month=...
```

#### Scenario: 获取所有仓库月度基线

- **WHEN** 调用 `getMonthlyBaseline(undefined, 2025, 3)`
- **THEN** 发送 `GET /api/baseline/monthly?year=2025&month=3`，返回 `MonthlyBaselineVO[]`

### Requirement: Impact API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 获取影响因素排序
export function getFactors(warehouseCode: string): Promise<FactorRankVO[]>
// GET /api/impact/factors?warehouseCode=...

// 获取相关性矩阵
export function getCorrelation(warehouseCode: string): Promise<CorrelationMatrixVO>
// GET /api/impact/correlation?warehouseCode=...
```

#### Scenario: 获取因素排序

- **WHEN** 调用 `getFactors('12000004')`
- **THEN** 发送 `GET /api/impact/factors?warehouseCode=12000004`，返回 `FactorRankVO[]`

### Requirement: Estimate API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 计算成本估算
export function calculate(request: EstimateRequest): Promise<EstimateResultVO>
// POST /api/estimate/calculate

// 获取估算默认参数
export function getEstimateDefaults(warehouseCode: string): Promise<EstimateRequest>
// GET /api/estimate/defaults/{warehouseCode}
```

#### Scenario: 提交成本估算

- **WHEN** 调用 `calculate({ dailyOrders: 500, itemsPerOrder: 4.25, ... })`
- **THEN** 发送 `POST /api/estimate/calculate`，请求体为 JSON，返回 `EstimateResultVO`

### Requirement: Report API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 生成报告
export function generateReport(request: ReportGenerateRequest): Promise<ReportVO>
// POST /api/report/generate

// 获取报告列表
export function getReportList(): Promise<ReportVO[]>
// GET /api/report/list

// 获取报告详情
export function getReportDetail(id: string): Promise<ReportVO>
// GET /api/report/{id}
```

#### Scenario: 生成报告

- **WHEN** 调用 `generateReport({ warehouseCode: '12000004', startMonth: '2025-03', endMonth: '2025-08' })`
- **THEN** 发送 `POST /api/report/generate`，返回 `ReportVO` 包含 `id`、`title`、`createdAt`

### Requirement: Warehouse API 方法

系统 SHALL 提供以下 API 方法：

```ts
// 获取仓库列表
export function getWarehouses(): Promise<WarehouseVO[]>
// GET /api/warehouses
```

#### Scenario: 获取仓库列表

- **WHEN** 调用 `getWarehouses()`
- **THEN** 发送 `GET /api/warehouses`，返回 `WarehouseVO[]`，每项包含 `warehouseCode` 和 `warehouseName`
