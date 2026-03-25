## Context

Phase 1 已完成 Spring Boot 3 后端骨架：19 张表的 Entity/Mapper 层、MySQL 只读数据源、CORS 和分页插件。当前 `controller/`、`service/` 包为空，无 API 端点。

本次需要在已有骨架上构建完整的 API 层，覆盖 5 大功能模块 + 通用 API + Python 服务调用客户端。

数据源：
- MySQL 5.7 @ 10.126.50.199:3306，库名 `wh_op_baseline`，只读，19 张表
- 2 个仓库：天津武清佩森A仓 (库房编码 12000004)、常熟高新正创B仓 (库房编码 32050005)
- 数据时间范围：2025-03 和 2025-08

已有费用计算公式（硬编码）：
```
月度操作费用 = 预估月度总工时(h) × 加权平均单价(元/h) × (1 + 税率)
默认税率: 6%
加权平均单价 = 固定劳务占比 × 固定劳务单价 + 临时劳务占比 × 临时劳务单价
```

## Goals / Non-Goals

**Goals:**
- 实现 14 个 RESTful API 端点（T2.1–T2.14）
- 统一响应格式 `Result<T>`，全局异常处理
- Service 层封装业务逻辑（聚合查询、计算）
- AnalyticsClient 实现与 Python FastAPI 的 HTTP 调用（含优雅降级）
- DTO/VO 分离：DTO 接收请求参数，VO 返回响应数据

**Non-Goals:**
- 不开发前端页面
- 不实现 Python FastAPI 分析服务本身（Phase 3）
- 不做认证/权限控制（Phase 7）
- 不做 MySQL 查询性能优化（索引/分页，Phase 7）
- 不创建 MyBatis XML Mapper（使用 MyBatis-Plus 注解查询 + QueryWrapper）

## Decisions

### D1: 包结构扩展

在 `com.kejie.whop` 下新增：
```
com.kejie.whop/
├── controller/
│   ├── DashboardController.java
│   ├── BaselineController.java
│   ├── EstimateController.java
│   ├── ImpactController.java
│   ├── ReportController.java
│   └── WarehouseController.java
├── service/
│   ├── DashboardService.java
│   ├── BaselineService.java
│   ├── EstimateService.java
│   ├── ImpactService.java
│   ├── ReportService.java
│   └── WarehouseService.java
├── client/
│   └── AnalyticsClient.java
├── model/
│   ├── dto/          # 请求参数
│   │   ├── EstimateRequest.java
│   │   └── ReportGenerateRequest.java
│   ├── vo/           # 响应数据
│   │   ├── Result.java
│   │   ├── DashboardOverviewVO.java
│   │   ├── TrendDataVO.java
│   │   ├── MonthlyBaselineVO.java
│   │   ├── WarehouseDetailVO.java
│   │   ├── CompareResultVO.java
│   │   ├── EstimateResultVO.java
│   │   ├── FactorRankVO.java
│   │   ├── CorrelationMatrixVO.java
│   │   ├── ReportVO.java
│   │   └── WarehouseVO.java
│   └── entity/       # 已有 19 个 Entity
└── config/
    └── GlobalExceptionHandler.java
```

### D2: 统一响应格式

```java
@Data
public class Result<T> {
    private int code;        // 200=成功, 500=错误
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) { ... }
    public static <T> Result<T> error(String message) { ... }
}
```

所有 Controller 返回 `Result<T>`。

### D3: API 端点设计

#### Dashboard API
| 端点 | 方法 | 请求参数 | 返回 | 数据来源 |
|---|---|---|---|---|
| `/api/dashboard/overview` | GET | `?warehouseCode=&month=` | `DashboardOverviewVO` | MySQL 聚合 |
| `/api/dashboard/trend` | GET | `?warehouseCode=&startMonth=&endMonth=&type=` | `List<TrendDataVO>` | MySQL 聚合 |

**DashboardOverviewVO** 包含：
- `totalOrders` — 总出库单量
- `totalWorkHours` — 总工时（小时）
- `monthlyFee` — 月度操作费用（元）
- `laborEfficiency` — 人效（单/人/天）
- `avgCostPerOrder` — 单均成本
- `avgCostPerItem` — 件均成本

**TrendDataVO** 包含：`date`, `warehouseCode`, `warehouseName`, `value`, `type`（outbound_orders / fee / workload_distribution）

#### 费用基线 API
| 端点 | 方法 | 请求参数 | 返回 | 数据来源 |
|---|---|---|---|---|
| `/api/baseline/monthly` | GET | `?warehouseCode=&year=&month=` | `List<MonthlyBaselineVO>` | MySQL 聚合计算 |
| `/api/baseline/warehouse/{warehouseCode}` | GET | `?year=&month=` | `WarehouseDetailVO` | MySQL 聚合 |
| `/api/baseline/compare` | GET | `?codes=A,B&year=&month=` | `List<CompareResultVO>` | MySQL 聚合 |

**MonthlyBaselineVO** 包含：
- `warehouseCode`, `warehouseName`, `year`, `month`
- `dailyAvgFee` — 日均费用
- `totalFee` — 月度总费用
- `totalOrders` — 总出库单量
- `totalItems` — 总出库件数
- `costPerOrder` — 单均成本
- `costPerItem` — 件均成本
- `avgHeadcount` — 日均出勤人数
- `totalWorkHours` — 总工时
- `weightedUnitPrice` — 加权平均劳务单价

#### 成本估算 API
| 端点 | 方法 | 请求/返回 | 说明 |
|---|---|---|---|
| `POST /api/estimate/calculate` | POST | `EstimateRequest` → `EstimateResultVO` | 公式硬编码计算 |
| `GET /api/estimate/defaults/{warehouseCode}` | GET | → `EstimateRequest` | 返回该仓历史平均值作为默认参数 |

**EstimateRequest** 包含：
- `dailyOrders` — 日均单量
- `itemsPerOrder` — 件单比
- `workDays` — 月工作天数
- `laborEfficiency` — 人效（单/人/天）
- `fixedLaborPrice` — 固定劳务单价
- `tempLaborPrice` — 临时劳务单价
- `fixedLaborRatio` — 固定劳务占比
- `taxRate` — 税率（默认 0.06）

**EstimateResultVO** 包含：
- `estimatedHeadcount` — 预估人数
- `estimatedTotalHours` — 预估月度总工时
- `weightedUnitPrice` — 加权平均单价
- `monthlyFee` — 月度操作费用
- `costPerOrder` — 单均成本
- `costPerItem` — 件均成本

计算逻辑：
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

#### 影响因素 API（代理 Python）
| 端点 | 方法 | 返回 | 数据来源 |
|---|---|---|---|
| `GET /api/impact/factors` | GET | `List<FactorRankVO>` | Python `/api/impact/factors` |
| `GET /api/impact/correlation` | GET | `CorrelationMatrixVO` | Python `/api/impact/correlation` |

请求参数：`?warehouseCode=`

AnalyticsClient 调用失败时返回空数据 + 错误提示（优雅降级，不抛异常）。

#### 报告 API
| 端点 | 方法 | 说明 |
|---|---|---|
| `POST /api/report/generate` | POST | 接收 `ReportGenerateRequest`（warehouseCode, startMonth, endMonth），调用 Service 汇总数据后生成 Markdown 文本，存储到内存 Map |
| `GET /api/report/list` | GET | 返回所有已生成报告摘要 |
| `GET /api/report/{id}` | GET | 返回指定报告内容（Markdown 文本） |

第一版报告存储在 Java 内存中（ConcurrentHashMap），不持久化。

#### 通用 API
| 端点 | 方法 | 说明 |
|---|---|---|
| `GET /api/warehouses` | GET | 返回仓库列表（warehouseCode + warehouseName），从出库单表中聚合去重获取 |

### D4: AnalyticsClient 设计

使用 Spring `RestClient`（Spring Boot 3.2+ 推荐）而非 WebFlux WebClient，避免引入 reactive 依赖。

```java
@Component
public class AnalyticsClient {
    private final RestClient restClient;

    public AnalyticsClient(@Value("${analytics.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<FactorRankVO> getFactors(String warehouseCode) { ... }
    public CorrelationMatrixVO getCorrelation(String warehouseCode) { ... }
}
```

优雅降级：try-catch 包裹，失败时记录日志并返回空数据。

### D5: Service 层查询策略

- **聚合查询**: 使用 MyBatis-Plus `QueryWrapper` + `selectMaps` 进行 GROUP BY 聚合
- **大表优化**: 首版不做索引优化（Phase 7 处理），但 WHERE 条件始终包含仓库编码和时间范围
- **仓库识别**: 通过出库单表的 `库房编码` 和 `库房名称` 字段区分仓库
- **工时计算**: 从出勤统计表的 `工作时长` 字段聚合

### D6: 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(e.getMessage());
    }
}
```

## Risks / Trade-offs

- **[大表聚合性能]** — 出勤统计表、出库单表、工作量表数据量大，首版无索引优化可能较慢。Phase 7 专项处理，当前可接受。
- **[Python 服务未就绪]** — 影响因素 API 依赖 Python FastAPI（Phase 3），首版使用 Mock 数据或优雅降级返回空结果。
- **[报告存储非持久化]** — 报告存储在 Java 内存，重启丢失。第一版 MVP 可接受，后续可改为 SQLite 或文件系统。
- **[仓库列表硬编码风险]** — 从出库单表聚合获取仓库列表，数据表为空时无法获取。可考虑 application.yml 中配置备选列表。
- **[费用公式硬编码]** — 费用估算公式固定在代码中，如需调整需修改代码。第一版可接受。
