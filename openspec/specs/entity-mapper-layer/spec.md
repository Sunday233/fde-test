## ADDED Requirements

### Requirement: 19 张表 Entity 实体类

系统 SHALL 为 MySQL `wh_op_baseline` 库中的 19 张表创建对应的 MyBatis-Plus Entity 实体类，统一放在 `com.kejie.whop.model.entity` 包下。

所属服务：backend  
数据库操作：只读 MySQL

#### Scenario: Entity 类注解规范
- **WHEN** 查看任意 Entity 类
- **THEN** SHALL 使用 `@TableName("实际表名")` 注解标注 MySQL 表名
- **THEN** SHALL 使用 `@TableId(type = IdType.AUTO)` 标注主键字段
- **THEN** SHALL 使用 Lombok `@Data` 注解减少样板代码
- **THEN** 主键字段类型 SHALL 为 `Long`

#### Scenario: 字段类型映射正确
- **WHEN** Entity 字段映射到 MySQL 列
- **THEN** `bigint` SHALL 映射为 `Long`
- **THEN** `varchar` SHALL 映射为 `String`
- **THEN** `datetime` SHALL 映射为 `LocalDateTime`
- **THEN** `date` SHALL 映射为 `LocalDate`
- **THEN** `decimal` SHALL 映射为 `BigDecimal`
- **THEN** `int` SHALL 映射为 `Integer`
- **THEN** `text` SHALL 映射为 `String`

#### Scenario: 19 张表 Entity 完整列表
- **WHEN** 查看 `model/entity/` 包
- **THEN** SHALL 包含以下 Entity 类：
  - `QuotationInfo` — 报价信息表 (`quotation_info`)
  - `WarehouseInventoryInfo` — 仓位库存信息表 (`warehouse_inventory_info`)
  - `WarehousePositionInfo` — 仓位信息表 (`warehouse_position_info`)
  - `OutboundOrder` — 出库单表 (`outbound_order`)
  - `AttendanceStatistics` — 出勤统计表 (`attendance_statistics`)
  - `VerificationOperation` — 复核操作表 (`verification_operation`)
  - `WorkloadStatisticsInfo` — 工作量统计信息表 (`workload_statistics_info`)
  - `WorkloadStatisticsDetail` — 工作量统计操作明细表 (`workload_statistics_detail`)
  - `WarehouseMovementExport` — 库内移动导出表 (`warehouse_movement_export`)
  - `InboundOrder` — 入库单表 (`inbound_order`)
  - `InboundOrderDetail` — 入库单行明细表 (`inbound_order_detail`)
  - `ShelvingOrder` — 上架单表 (`shelving_order`)
  - `ShelvingOrderDetail` — 上架单明细表 (`shelving_order_detail`)
  - `ReturnInfo` — 退货信息表 (`return_info`)
  - `MaterialBasicInfo` — 物料基本信息表 (`material_basic_info`)
  - `FixedAssetDetail` — 在账资产明细表 (`fixed_asset_detail`)
  - `LeasedAssetInventory` — 租赁资产库存导出表 (`leased_asset_inventory`)
  - `PickingOperation` — 拣货操作表 (`picking_operation`)
  - `PickingOperationDetail` — 拣货操作明细表 (`picking_operation_detail`)

### Requirement: 19 张表 Mapper 接口

系统 SHALL 为每张表创建对应的 MyBatis-Plus Mapper 接口，继承 `BaseMapper<T>`，统一放在 `com.kejie.whop.mapper` 包下。

所属服务：backend

#### Scenario: Mapper 接口规范
- **WHEN** 查看任意 Mapper 接口
- **THEN** SHALL 使用 `@Mapper` 注解
- **THEN** SHALL 继承 `BaseMapper<对应Entity>`
- **THEN** 本阶段 SHALL 不包含自定义方法（Phase 2 按需添加）

#### Scenario: 19 个 Mapper 完整列表
- **WHEN** 查看 `mapper/` 包
- **THEN** SHALL 包含 19 个 Mapper 接口，与 Entity 一一对应：
  - `QuotationInfoMapper`
  - `WarehouseInventoryInfoMapper`
  - `WarehousePositionInfoMapper`
  - `OutboundOrderMapper`
  - `AttendanceStatisticsMapper`
  - `VerificationOperationMapper`
  - `WorkloadStatisticsInfoMapper`
  - `WorkloadStatisticsDetailMapper`
  - `WarehouseMovementExportMapper`
  - `InboundOrderMapper`
  - `InboundOrderDetailMapper`
  - `ShelvingOrderMapper`
  - `ShelvingOrderDetailMapper`
  - `ReturnInfoMapper`
  - `MaterialBasicInfoMapper`
  - `FixedAssetDetailMapper`
  - `LeasedAssetInventoryMapper`
  - `PickingOperationMapper`
  - `PickingOperationDetailMapper`
