## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: 健康检查端点

系统 SHALL 提供 `GET /api/health` 端点，返回服务运行状态和依赖项连通性。

**路由**: `routers/health.py`，前缀 `/api`

**响应格式**:
```json
{
  "status": "healthy",
  "mysql": true,
  "sqlite": true,
  "last_precompute": "2025-03-25T10:00:00",
  "version": "0.1.0"
}
```

**检查项**:
- `mysql`: 尝试执行 `SELECT 1`，成功返回 `true`，失败返回 `false`
- `sqlite`: 检查 `results.db` 文件是否存在且可读
- `last_precompute`: 从 SQLite 查询最近的 `computed_at` 时间，无数据时返回 `null`
- `status`: 当 mysql 和 sqlite 均为 true 时返回 `"healthy"`，否则返回 `"degraded"`

**HTTP 状态码**: 始终返回 200（即使降级），用于 Docker 健康检查

#### Scenario: 所有依赖正常

- **WHEN** MySQL 可连接且 SQLite 文件存在，调用 `GET /api/health`
- **THEN** 返回 HTTP 200，`status` 为 `"healthy"`，`mysql` 和 `sqlite` 均为 `true`

#### Scenario: MySQL 不可达

- **WHEN** MySQL 服务器不可达，调用 `GET /api/health`
- **THEN** 返回 HTTP 200，`status` 为 `"degraded"`，`mysql` 为 `false`，`sqlite` 为 `true`

#### Scenario: 首次启动无预计算数据

- **WHEN** 服务刚启动尚未完成首次预计算，调用 `GET /api/health`
- **THEN** `last_precompute` 为 `null`，其他字段正常返回
