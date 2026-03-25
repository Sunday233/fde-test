## MODIFIED Requirements

### Requirement: Python 分析服务 HTTP 客户端

系统 SHALL 提供 `AnalyticsClient` 组件，通过 Spring `RestClient` 调用 Python FastAPI 分析服务的 HTTP API。

**实现层**: `com.kejie.whop.client.AnalyticsClient`
- 使用 Spring Boot 3.2+ `RestClient`（非 WebFlux WebClient），无需引入 reactive 依赖
- 注入 `analytics.base-url` 配置项（默认值 `http://localhost:8000`）
- 提供两个公开方法：
  - `getFactors(String warehouseCode)` → `List<FactorRankVO>`
  - `getCorrelation(String warehouseCode)` → `CorrelationMatrixVO`

**配置**: application.yml 新增：
```yaml
analytics:
  base-url: ${ANALYTICS_BASE_URL:http://localhost:8000}
```

**优雅降级策略**:
- 所有调用 MUST 在 try-catch 中执行
- 捕获所有异常（连接超时、HTTP 错误、解析失败）
- 失败时记录 WARN 级别日志（包含异常信息和目标 URL）
- 返回空数据（空列表或空矩阵），不抛出异常

**调用的 Python API 端点**（明确 Python 端响应契约）:
- `GET /api/impact/factors?warehouseCode={code}` → Python 端从 SQLite impact_results 查询，返回 JSON 数组 `[{rank, factorName, correlation, description}]`
- `GET /api/impact/correlation?warehouseCode={code}` → Python 端从 SQLite impact_results 查询 matrix_json，返回 JSON 对象 `{factors, matrix}`

**Python 端 JSON 字段命名约定**: 使用 camelCase（由 Pydantic model_config `alias_generator` 实现），确保与 Java VO 自动反序列化兼容

#### Scenario: Python 服务正常调用因素排序

- **WHEN** `AnalyticsClient.getFactors("12000004")` 被调用，且 Python 服务正常返回 JSON 数组
- **THEN** 方法返回 `List<FactorRankVO>`，元素数量和内容与 Python 响应一致

#### Scenario: Python 服务不可达时优雅降级

- **WHEN** Python 服务未启动或网络不通，`AnalyticsClient.getFactors(...)` 被调用
- **THEN** 方法返回空列表 `Collections.emptyList()`，日志输出 WARN 级别信息

#### Scenario: Python 服务正常调用相关性矩阵

- **WHEN** `AnalyticsClient.getCorrelation("12000004")` 被调用，且 Python 服务正常返回 JSON 对象
- **THEN** 方法返回 `CorrelationMatrixVO`，`factors` 列表包含 10 个因素名称，`matrix` 为 10×10 二维数组
