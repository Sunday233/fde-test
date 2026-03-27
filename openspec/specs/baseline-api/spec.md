## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 月度费用基线汇总接口

系统 SHALL 提供 `GET /api/baseline/monthly` 接口，返回指定月份区间内的仓库月度费用基线汇总。

**Controller 层**: `BaselineController.monthly(String warehouseCode, String startMonth, String endMonth, Integer page, Integer size)`  
**Service 层**: `BaselineService.getMonthlyBaseline(warehouseCode, startMonth, endMonth)`

接口 SHALL 支持以下查询参数：
- `warehouseCode`: 可选，过滤单仓
- `startMonth`: 可选，格式 `YYYY-MM`
- `endMonth`: 可选，格式 `YYYY-MM`
- `page`: 可选，分页页码
- `size`: 可选，分页大小

服务 SHALL 在给定月份区间内逐月生成结果，并返回每个仓库、每个月份的一条记录。主要字段包括：
- `year`
- `month`
- `dailyAvgFee`
- `totalFee`
- `totalOrders`
- `totalItems`
- `itemsPerOrder`
- `dailyAvgOrders`
- `costPerOrder`
- `costPerItem`
- `avgHeadcount`
- `laborEfficiency`
- `fixedTempRatio`
- `totalWorkHours`
- `weightedUnitPrice`

#### Scenario: 查询最近区间月度基线
- **WHEN** 客户端请求 `GET /api/baseline/monthly?startMonth=2024-10&endMonth=2025-09`
- **THEN** 系统返回区间内所有仓库的月度基线数据
- **AND** 返回结果中的每条记录 SHALL 带有 `year` 与 `month`

#### Scenario: 按仓库筛选区间月度基线
- **WHEN** 客户端请求 `GET /api/baseline/monthly?warehouseCode=12000004&startMonth=2025-01&endMonth=2025-06`
- **THEN** 系统仅返回该仓库在区间内的月度基线记录

### Requirement: 仓库费用详情接口

系统 SHALL 提供 `GET /api/baseline/warehouse/{warehouseCode}` 接口，返回指定仓库的详细费用分析数据。

**Controller 层**: `BaselineController.warehouseDetail(String warehouseCode, Integer year, Integer month)`  
**Service 层**: `BaselineService.getWarehouseDetail(warehouseCode, year, month)`

该接口保持单月明细模式，返回月度费用分析详情，包括工时构成与劳务分布。

#### Scenario: 查询仓库费用详情
- **WHEN** 客户端请求 `GET /api/baseline/warehouse/12000004?year=2025&month=3`
- **THEN** 系统返回该仓库该月份的详细费用分析结果

### Requirement: 仓库费用区间对比接口

系统 SHALL 提供 `GET /api/baseline/compare` 接口，返回多个仓库在指定月份区间内的月度对比数据。

**Controller 层**: `BaselineController.compare(String codes, String startMonth, String endMonth)`  
**Service 层**: `BaselineService.compare(codes, startMonth, endMonth)`

接口 SHALL 支持：
- `codes`: 逗号分隔的仓库编码列表
- `startMonth`: 起始月份，格式 `YYYY-MM`
- `endMonth`: 结束月份，格式 `YYYY-MM`

服务 SHALL 对每个仓库、区间内每个月分别输出一条 `CompareResultVO`，至少包含以下字段：
- `year`
- `month`
- `warehouseCode`
- `warehouseName`
- `totalFee`
- `totalOrders`
- `avgHeadcount`
- `dailyAvgFee`
- `costPerOrder`
- `costPerItem`
- `laborEfficiency`
- `totalWorkHours`

该接口 SHALL 用于双仓月度趋势图，而不是单月静态横向对比。

#### Scenario: 双仓区间对比
- **WHEN** 客户端请求 `GET /api/baseline/compare?codes=12000004,32050005&startMonth=2025-01&endMonth=2025-06`
- **THEN** 系统返回两仓在 2025-01 至 2025-06 每个月的对比记录
- **AND** 客户端可以按 `year + month` 聚合绘制趋势图

#### Scenario: 单仓退化对比
- **WHEN** 客户端请求 `GET /api/baseline/compare?codes=12000004&startMonth=2025-01&endMonth=2025-06`
- **THEN** 系统返回该仓库在区间内的月度对比记录

### Requirement: 每日操作费用明细接口

系统 SHALL 提供 `GET /api/baseline/dailyDetail` 接口，返回指定仓库和月份区间内的每日操作费用明细。

**Controller 层**: `BaselineController.dailyDetail(String warehouseCode, String startMonth, String endMonth)`  
**Service 层**: `BaselineService.getDailyDetail(warehouseCode, startMonth, endMonth)`

服务 SHALL 聚合每日出库与出勤数据，并输出以下字段：
- `date`
- `warehouseCode`
- `warehouseName`
- `obOrders`
- `obItems`
- `itemOrderRatio`
- `headcount`
- `workHours`
- `dailyFee`

其中：
- `itemOrderRatio = obItems / obOrders`
- `dailyFee = workHours × weightedUnitPrice × 1.06`

#### Scenario: 查询每日费用明细
- **WHEN** 客户端请求 `GET /api/baseline/dailyDetail?startMonth=2025-03&endMonth=2025-03`
- **THEN** 系统返回该月份内所有仓库的按日费用明细

#### Scenario: 按仓库筛选每日费用明细
- **WHEN** 客户端请求 `GET /api/baseline/dailyDetail?warehouseCode=12000004&startMonth=2025-03&endMonth=2025-04`
- **THEN** 系统仅返回该仓库在区间内的每日费用明细