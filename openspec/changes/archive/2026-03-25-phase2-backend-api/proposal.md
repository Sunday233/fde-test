## Why

Phase 1 完成了 Spring Boot 3 后端骨架（Entity/Mapper 层），但尚无任何 API 端点，前端和分析服务无法获取数据。Phase 2 需要为 5 大功能模块（Dashboard、费用基线、影响因素、成本估算、报告）提供完整的 RESTful API 层，同时实现对 Python FastAPI 分析服务的 HTTP 调用能力，使整个平台数据链路贯通。

## What Changes

- 新增 Dashboard API：核心 KPI 概览（总单量、总工时、月度费用、人效）+ 多维趋势数据（日出库量、月费用、工作量分布）
- 新增费用基线 API：月度费用基线汇总、单仓详情、双仓/多仓对比
- 新增成本估算 API：费用估算计算（公式硬编码）+ 历史默认参数
- 新增影响因素 API：代理 Python 分析服务的影响因素排序和相关性矩阵
- 新增报告 API：生成 Markdown 报告、报告列表、报告内容查看
- 新增仓库列表通用 API
- 新增 `AnalyticsClient` 用于 HTTP 调用 Python FastAPI 服务
- 新增 Service 层（业务逻辑聚合 MySQL 查询和分析服务结果）
- 新增统一响应格式 `Result<T>` 和全局异常处理

## Capabilities

### New Capabilities
- `dashboard-api`: Dashboard 核心 KPI 概览和多维趋势数据 API（T2.1、T2.2）
- `baseline-api`: 费用基线月度汇总、单仓详情、多仓对比 API（T2.3、T2.4、T2.5）
- `estimate-api`: 成本估算计算器 + 历史默认参数 API（T2.6、T2.7）
- `impact-api`: 影响因素排序和相关性矩阵 API，代理 Python 结果（T2.8、T2.9）
- `report-api`: 报告生成、列表、内容查看 API（T2.10、T2.11、T2.12）
- `warehouse-api`: 仓库列表通用 API（T2.13）
- `analytics-client`: Spring Boot 调用 Python FastAPI 的 HTTP 客户端（T2.14）

### Modified Capabilities
（无已有 spec 需要修改）

## Impact

- **后端代码**: 新增 Controller / Service / DTO / VO / Client 层，`com.kejie.whop` 下新增 `controller/`、`service/`、`model/dto/`、`model/vo/`、`client/` 包
- **依赖**: pom.xml 无需新增依赖（RestClient 已包含在 spring-boot-starter-web 中）
- **数据库**: 查询 MySQL 19 张表（只读），涉及聚合查询（按日/月/仓库维度）
- **外部服务**: 依赖 Python FastAPI 服务（尚未实现，影响因素 API 将优雅降级）
- **配置**: application.yml 需新增 `analytics.base-url` 配置项
