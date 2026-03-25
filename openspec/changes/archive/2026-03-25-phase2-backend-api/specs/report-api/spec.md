## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 报告生成接口

系统 SHALL 提供 `POST /api/report/generate` 接口，根据参数生成费用分析 Markdown 报告并存储在内存中。

**Controller 层**: `ReportController.generate(ReportGenerateRequest request)` → `Result<ReportVO>`

**Service 层**: `ReportService.generate(ReportGenerateRequest request)`
- 接收参数：warehouseCode, startMonth, endMonth
- 调用 BaselineService 获取指定仓库和时间范围的费用基线数据
- 调用 DashboardService 获取 KPI 概览
- 组装 Markdown 报告内容，包含：
  - 报告标题和生成时间
  - 仓库基本信息
  - 月度费用汇总表
  - KPI 指标对比
  - 费用组成分析
- 生成唯一 ID（UUID），存储到 ConcurrentHashMap
- 返回报告摘要信息

**Mapper 层**: 无（调用其他 Service，数据存储在 Java 内存中）

**请求示例**:
```
POST /api/report/generate
Content-Type: application/json

{
  "warehouseCode": "12000004",
  "startMonth": "2025-03",
  "endMonth": "2025-08"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "rpt-a1b2c3d4",
    "title": "天津武清佩森A仓 费用分析报告 (2025-03 ~ 2025-08)",
    "warehouseCode": "12000004",
    "warehouseName": "天津武清佩森A仓",
    "startMonth": "2025-03",
    "endMonth": "2025-08",
    "createdAt": "2025-03-26T10:30:00"
  }
}
```

#### Scenario: 正常生成报告

- **WHEN** 客户端提交 `POST /api/report/generate`，请求体包含 `warehouseCode=12000004`、`startMonth=2025-03`、`endMonth=2025-08`
- **THEN** 系统返回 `code=200`，`data` 包含新生成报告的 `id`、`title`、`createdAt` 等摘要信息，报告内容存储在内存中

#### Scenario: 缺少必要参数

- **WHEN** 客户端提交 `POST /api/report/generate` 时缺少 `warehouseCode`
- **THEN** 系统返回 `code=500`，`message` 包含参数校验错误信息

### Requirement: 报告列表接口

系统 SHALL 提供 `GET /api/report/list` 接口，返回所有已生成报告的摘要列表。

**Controller 层**: `ReportController.list()` → `Result<List<ReportVO>>`

**Service 层**: `ReportService.list()`
- 从 ConcurrentHashMap 中读取所有报告的摘要信息
- 按创建时间倒序排列

**请求示例**:
```
GET /api/report/list
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "rpt-a1b2c3d4",
      "title": "天津武清佩森A仓 费用分析报告 (2025-03 ~ 2025-08)",
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓",
      "startMonth": "2025-03",
      "endMonth": "2025-08",
      "createdAt": "2025-03-26T10:30:00"
    }
  ]
}
```

#### Scenario: 查询报告列表

- **WHEN** 客户端请求 `GET /api/report/list`
- **THEN** 系统返回 `code=200`，`data` 为报告摘要数组，按 `createdAt` 倒序排列

#### Scenario: 无报告时查询列表

- **WHEN** 客户端请求 `GET /api/report/list`，且尚未生成任何报告
- **THEN** 系统返回 `code=200`，`data` 为空列表 `[]`

### Requirement: 报告内容接口

系统 SHALL 提供 `GET /api/report/{id}` 接口，返回指定报告的完整 Markdown 内容。

**Controller 层**: `ReportController.detail(String id)` → `Result<Map<String, Object>>`

**Service 层**: `ReportService.getById(id)`
- 从 ConcurrentHashMap 中按 ID 查找报告
- 报告不存在时返回 null

**请求示例**:
```
GET /api/report/rpt-a1b2c3d4
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "rpt-a1b2c3d4",
    "title": "天津武清佩森A仓 费用分析报告 (2025-03 ~ 2025-08)",
    "content": "# 天津武清佩森A仓 费用分析报告\n\n## 报告概述\n...\n\n## 月度费用汇总\n...",
    "createdAt": "2025-03-26T10:30:00"
  }
}
```

#### Scenario: 查询已有报告

- **WHEN** 客户端请求 `GET /api/report/rpt-a1b2c3d4`，且该报告存在
- **THEN** 系统返回 `code=200`，`data` 包含 `id`、`title`、`content`（Markdown 文本）、`createdAt`

#### Scenario: 查询不存在的报告

- **WHEN** 客户端请求 `GET /api/report/not-exist-id`
- **THEN** 系统返回 `code=200`，`data` 为 null
