## 1. Maven 项目骨架

- [x] 1.1 在 `wh-op-platform/backend/` 下创建 `pom.xml`，声明 Spring Boot 3.4.x parent、Java 21、groupId `com.kejie`、artifactId `wh-op-platform`
- [x] 1.2 添加依赖：`spring-boot-starter-web`、`mybatis-plus-spring-boot3-starter`（3.5.9+）、`mysql-connector-j`(runtime)、`lombok`(provided)、`spring-boot-starter-validation`
- [x] 1.3 创建目录结构：`src/main/java/com/kejie/whop/`，及子包 `config/`、`model/entity/`、`mapper/`、`controller/`、`service/`、`util/`
- [x] 1.4 创建启动类 `WhOpApplication.java`（`@SpringBootApplication` + `@MapperScan("com.kejie.whop.mapper")`）

## 2. 配置文件

- [x] 2.1 创建 `src/main/resources/application.yml`，配置 MySQL 数据源（环境变量注入、HikariCP 只读连接池）
- [x] 2.2 在 application.yml 中配置 MyBatis-Plus（驼峰映射、主键自增、日志级别）
- [x] 2.3 在 application.yml 中配置 server.port=8080

## 3. CORS 和 MyBatis-Plus 配置类

- [x] 3.1 创建 `CorsConfig.java`（`/api/**` 路径允许跨域，所有来源/方法/请求头）
- [x] 3.2 创建 `MyBatisPlusConfig.java`（注册分页插件 `PaginationInnerInterceptor`，指定 `DbType.MYSQL`）

## 4. Entity 实体类（19 张表）

- [x] 4.1 创建入库流程 Entity：`InboundOrder`（入库单表）、`InboundOrderDetail`（入库单行明细表）
- [x] 4.2 创建上架流程 Entity：`ShelvingOrder`（上架单表）、`ShelvingOrderDetail`（上架单明细表）
- [x] 4.3 创建出库流程 Entity：`OutboundOrder`（出库单表）
- [x] 4.4 创建操作记录 Entity：`PickingOperation`（拣货操作表）、`PickingOperationDetail`（拣货操作明细表）、`VerificationOperation`（复核操作表）
- [x] 4.5 创建工作量统计 Entity：`WorkloadStatisticsInfo`（工作量统计信息表）、`WorkloadStatisticsDetail`（工作量统计操作明细表）
- [x] 4.6 创建仓位管理 Entity：`WarehousePositionInfo`（仓位信息表）、`WarehouseInventoryInfo`（仓位库存信息表）、`WarehouseMovementExport`（库内移动导出表）
- [x] 4.7 创建人员与退货 Entity：`AttendanceStatistics`（出勤统计表）、`ReturnInfo`（退货信息表）
- [x] 4.8 创建主数据 Entity：`QuotationInfo`（报价信息表）、`MaterialBasicInfo`（物料基本信息表）
- [x] 4.9 创建资产管理 Entity：`FixedAssetDetail`（在账资产明细表）、`LeasedAssetInventory`（租赁资产库存导出表）

## 5. Mapper 接口（19 张表）

- [x] 5.1 创建全部 19 个 Mapper 接口，每个继承 `BaseMapper<对应Entity>`，使用 `@Mapper` 注解

## 6. Dockerfile

- [x] 6.1 创建 `backend/Dockerfile`：多阶段构建（maven:3.9-eclipse-temurin-21 构建 + eclipse-temurin:21-jre 运行），暴露 8080 端口

## 7. 验证

- [x] 7.1 执行 `mvn compile` 验证项目编译成功
- [x] 7.2 删除 `backend/.gitkeep`（已被项目文件替代）
- [x] 7.3 Git 提交：`feat: Phase 1 - Spring Boot 3 后端项目骨架搭建`
