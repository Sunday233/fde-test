## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 影响因素排序接口

系统 SHALL 提供 `GET /api/impact/factors` 接口，返回影响操作费用的因素排序列表。数据来源为 Python FastAPI 分析服务。

**Controller 层**: `ImpactController.factors(String warehouseCode)` → `Result<List<FactorRankVO>>`

**Service 层**: `ImpactService.getFactors(warehouseCode)`
- 调用 `AnalyticsClient.getFactors(warehouseCode)` 获取 Python 服务的分析结果
- 若 Python 服务不可用，返回空列表 + 警告消息（优雅降级）

**Mapper 层**: 无（数据来源为 Python 服务 HTTP 调用）

**请求示例**:
```
GET /api/impact/factors?warehouseCode=12000004
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "rank": 1,
      "factorName": "出勤人数",
      "correlation": 0.96,
      "description": "日出勤人数与日操作费用强正相关"
    },
    {
      "rank": 2,
      "factorName": "临时劳务人数",
      "correlation": 0.87,
      "description": "临时劳务人数与操作费用高度正相关"
    },
    {
      "rank": 3,
      "factorName": "固定劳务人数",
      "correlation": 0.78,
      "description": "固定劳务人数与操作费用正相关"
    }
  ]
}
```

#### Scenario: Python 服务正常响应

- **WHEN** 客户端请求 `GET /api/impact/factors?warehouseCode=12000004`，且 Python 分析服务正常运行
- **THEN** 系统返回 `code=200`，`data` 为按 `correlation` 降序排列的因素列表，每个元素包含 `rank`、`factorName`、`correlation`、`description`

#### Scenario: Python 服务不可用（优雅降级）

- **WHEN** 客户端请求 `GET /api/impact/factors?warehouseCode=12000004`，但 Python 分析服务无法连接
- **THEN** 系统返回 `code=200`，`data` 为空列表 `[]`，`message` 包含"分析服务暂不可用"提示

### Requirement: 相关性矩阵接口

系统 SHALL 提供 `GET /api/impact/correlation` 接口，返回费用影响因素的相关性矩阵（Pearson 相关系数矩阵）。

**Controller 层**: `ImpactController.correlation(String warehouseCode)` → `Result<CorrelationMatrixVO>`

**Service 层**: `ImpactService.getCorrelation(warehouseCode)`
- 调用 `AnalyticsClient.getCorrelation(warehouseCode)` 获取 Python 服务的分析结果
- 若 Python 服务不可用，返回空矩阵 + 警告消息

**请求示例**:
```
GET /api/impact/correlation?warehouseCode=12000004
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "factors": ["出勤人数", "临时劳务人数", "固定劳务人数", "出库单量", "出库件数"],
    "matrix": [
      [1.00, 0.87, 0.78, 0.65, 0.72],
      [0.87, 1.00, 0.45, 0.58, 0.61],
      [0.78, 0.45, 1.00, 0.52, 0.55],
      [0.65, 0.58, 0.52, 1.00, 0.89],
      [0.72, 0.61, 0.55, 0.89, 1.00]
    ]
  }
}
```

#### Scenario: 正常获取相关性矩阵

- **WHEN** 客户端请求 `GET /api/impact/correlation?warehouseCode=12000004`，且 Python 服务正常
- **THEN** 系统返回 `code=200`，`data` 包含 `factors`（因素名称列表）和 `matrix`（对称方阵，对角线为 1.0）

#### Scenario: Python 服务不可用（优雅降级）

- **WHEN** 客户端请求 `GET /api/impact/correlation?warehouseCode=12000004`，但 Python 服务不可用
- **THEN** 系统返回 `code=200`，`data` 中 `factors` 为空列表、`matrix` 为空列表，`message` 包含"分析服务暂不可用"提示
