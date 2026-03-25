## 1. 通用基础设施

- [x] 1.1 创建 `Result<T>` 统一响应包装类 (`model/vo/Result.java`)
- [x] 1.2 创建 `GlobalExceptionHandler` 全局异常处理器 (`config/GlobalExceptionHandler.java`)
- [x] 1.3 在 `application.yml` 中添加 `analytics.base-url` 配置项

## 2. DTO / VO 模型类

- [x] 2.1 创建 Dashboard VO：`DashboardOverviewVO.java`、`TrendDataVO.java`
- [x] 2.2 创建 Baseline VO：`MonthlyBaselineVO.java`、`WarehouseDetailVO.java`、`CompareResultVO.java`
- [x] 2.3 创建 Estimate DTO/VO：`EstimateRequest.java`（含 @Valid 校验）、`EstimateResultVO.java`
- [x] 2.4 创建 Impact VO：`FactorRankVO.java`、`CorrelationMatrixVO.java`
- [x] 2.5 创建 Report DTO/VO：`ReportGenerateRequest.java`（含 @Valid 校验）、`ReportVO.java`
- [x] 2.6 创建 Warehouse VO：`WarehouseVO.java`

## 3. AnalyticsClient（Python 服务调用）

- [x] 3.1 创建 `AnalyticsClient.java` (`client/AnalyticsClient.java`)，使用 RestClient 调用 Python API，实现 `getFactors()` 和 `getCorrelation()` 方法，含 try-catch 优雅降级

## 4. Service 层实现

- [x] 4.1 创建 `WarehouseService.java`：从出库单表 DISTINCT 查询仓库列表
- [x] 4.2 创建 `DashboardService.java`：实现 `getOverview()` 和 `getTrend()` 方法，聚合出库单表 + 出勤统计表 + 报价信息表
- [x] 4.3 创建 `BaselineService.java`：实现 `getMonthlyBaseline()`、`getWarehouseDetail()`、`compare()` 方法，含费用计算公式
- [x] 4.4 创建 `EstimateService.java`：实现 `calculate()` 计算逻辑和 `getDefaults()` 历史默认值查询
- [x] 4.5 创建 `ImpactService.java`：调用 AnalyticsClient 获取影响因素数据，带优雅降级
- [x] 4.6 创建 `ReportService.java`：实现报告生成（Markdown 组装）、列表、详情查询（ConcurrentHashMap 存储）

## 5. Controller 层实现

- [x] 5.1 创建 `WarehouseController.java`：`GET /api/warehouses`
- [x] 5.2 创建 `DashboardController.java`：`GET /api/dashboard/overview`、`GET /api/dashboard/trend`
- [x] 5.3 创建 `BaselineController.java`：`GET /api/baseline/monthly`、`GET /api/baseline/warehouse/{warehouseCode}`、`GET /api/baseline/compare`
- [x] 5.4 创建 `EstimateController.java`：`POST /api/estimate/calculate`、`GET /api/estimate/defaults/{warehouseCode}`
- [x] 5.5 创建 `ImpactController.java`：`GET /api/impact/factors`、`GET /api/impact/correlation`
- [x] 5.6 创建 `ReportController.java`：`POST /api/report/generate`、`GET /api/report/list`、`GET /api/report/{id}`

## 6. 依赖更新与编译验证

- [x] 6.1 更新 `pom.xml`：确认 RestClient 所需依赖（spring-boot-starter-web 已包含）
- [x] 6.2 修正 proposal.md 中 webflux 依赖描述（实际使用 RestClient，无需 webflux）
- [x] 6.3 执行 `mvn compile` 验证全量编译通过
