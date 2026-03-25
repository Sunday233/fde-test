## ADDED Requirements

**所属服务**: backend (Spring Boot 3)

### Requirement: 仓库列表接口

系统 SHALL 提供 `GET /api/warehouses` 接口，返回所有仓库的基本信息列表。

**Controller 层**: `WarehouseController.list()` → `Result<List<WarehouseVO>>`

**Service 层**: `WarehouseService.list()`
- 从出库单表（`outbound_order`）按 `库房编码` 和 `库房名称` 进行 DISTINCT 查询
- 返回仓库编码和名称列表

**Mapper 层**: 使用 `QueryWrapper` + `selectMaps` 对出库单表进行 `SELECT DISTINCT 库房编码, 库房名称` 查询

**请求示例**:
```
GET /api/warehouses
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "warehouseCode": "12000004",
      "warehouseName": "天津武清佩森A仓"
    },
    {
      "warehouseCode": "32050005",
      "warehouseName": "常熟高新正创B仓"
    }
  ]
}
```

#### Scenario: 获取仓库列表

- **WHEN** 客户端请求 `GET /api/warehouses`
- **THEN** 系统返回 `code=200`，`data` 为仓库列表数组，每个元素包含 `warehouseCode` 和 `warehouseName`

#### Scenario: 数据库中无数据

- **WHEN** 客户端请求 `GET /api/warehouses`，但出库单表为空
- **THEN** 系统返回 `code=200`，`data` 为空列表 `[]`
