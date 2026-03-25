## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

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

**调用的 Python API 端点**:
- `GET /api/impact/factors?warehouseCode={code}` → 返回 JSON 数组
- `GET /api/impact/correlation?warehouseCode={code}` → 返回 JSON 对象

**请求/响应格式（Python 端）**:

获取因素排序：
```
GET http://localhost:8000/api/impact/factors?warehouseCode=12000004

Response:
[
  {"rank": 1, "factorName": "出勤人数", "correlation": 0.96, "description": "..."},
  {"rank": 2, "factorName": "临时劳务人数", "correlation": 0.87, "description": "..."}
]
```

获取相关性矩阵：
```
GET http://localhost:8000/api/impact/correlation?warehouseCode=12000004

Response:
{
  "factors": ["出勤人数", "临时劳务人数", "固定劳务人数"],
  "matrix": [[1.0, 0.87, 0.78], [0.87, 1.0, 0.45], [0.78, 0.45, 1.0]]
}
```

#### Scenario: Python 服务正常调用因素排序

- **WHEN** `AnalyticsClient.getFactors("12000004")` 被调用，且 Python 服务正常返回 JSON 数组
- **THEN** 方法返回 `List<FactorRankVO>`，元素数量和内容与 Python 响应一致

#### Scenario: Python 服务正常调用相关性矩阵

- **WHEN** `AnalyticsClient.getCorrelation("12000004")` 被调用，且 Python 服务正常返回 JSON 对象
- **THEN** 方法返回 `CorrelationMatrixVO`，其 `factors` 和 `matrix` 字段与 Python 响应一致

#### Scenario: Python 服务连接超时

- **WHEN** `AnalyticsClient.getFactors("12000004")` 被调用，但 Python 服务连接超时
- **THEN** 方法记录 WARN 日志并返回空列表 `Collections.emptyList()`，不抛出异常

#### Scenario: Python 服务返回 HTTP 500

- **WHEN** `AnalyticsClient.getCorrelation("12000004")` 被调用，但 Python 服务返回 500 错误
- **THEN** 方法记录 WARN 日志并返回空 `CorrelationMatrixVO`（factors 和 matrix 均为空列表），不抛出异常

### Requirement: 统一响应格式和全局异常处理

系统 SHALL 提供 `Result<T>` 统一响应包装类和 `GlobalExceptionHandler` 全局异常处理器。

**Result<T>**: `com.kejie.whop.model.vo.Result`
```java
@Data
public class Result<T> {
    private int code;        // 200=成功, 500=错误
    private String message;
    private T data;

    public static <T> Result<T> ok(T data);
    public static <T> Result<T> error(String message);
}
```
所有 Controller 方法的返回类型 MUST 为 `Result<T>`。

**GlobalExceptionHandler**: `com.kejie.whop.config.GlobalExceptionHandler`
- 使用 `@RestControllerAdvice` 注解
- 捕获 `MethodArgumentNotValidException`，返回 `code=500`，message 包含字段校验详情
- 捕获 `Exception`，返回 `code=500`，message 包含异常信息

#### Scenario: 正常响应包装

- **WHEN** Controller 方法正常执行并返回数据
- **THEN** 响应 JSON 格式为 `{"code": 200, "message": "success", "data": ...}`

#### Scenario: 参数校验失败

- **WHEN** Controller 接收到不合法的 `@Valid` 参数（如缺少必填字段）
- **THEN** `GlobalExceptionHandler` 捕获 `MethodArgumentNotValidException`，响应 `{"code": 500, "message": "参数校验失败: ...", "data": null}`

#### Scenario: 未预期异常

- **WHEN** Service 层抛出未捕获的 `RuntimeException`
- **THEN** `GlobalExceptionHandler` 捕获异常，响应 `{"code": 500, "message": "异常信息", "data": null}`
