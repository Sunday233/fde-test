## Context

Phase 0 已完成 Monorepo 根目录初始化（`wh-op-platform/`），`backend/` 目录当前仅有 `.gitkeep`。本次需要在其中搭建完整的 Spring Boot 3 项目骨架，包括 Maven 项目结构、MySQL 数据源配置、CORS 配置、19 张表的 Entity/Mapper 层，以及 Dockerfile。

数据库操作：**只读 MySQL**（远端 10.126.50.199:3306，库名 `wh_op_baseline`）

19 张表按业务域分组：
- **入库流程**: 入库单表 `inbound_order`、入库单行明细表 `inbound_order_detail`
- **上架流程**: 上架单表 `shelving_order`、上架单明细表 `shelving_order_detail`
- **出库流程**: 出库单表 `outbound_order`（758K 行）
- **操作记录**: 拣货操作表 `picking_operation`（574K 行）、拣货操作明细表 `picking_operation_detail`（483K 行）、复核操作表 `verification_operation`（2.04M 行）
- **工作量统计**: 工作量统计信息表 `workload_statistics_info`、工作量统计操作明细表 `workload_statistics_detail`（2.12M 行）
- **仓位管理**: 仓位信息表 `warehouse_position_info`（2.06M 行）、仓位库存信息表 `warehouse_inventory_info`、库内移动导出表 `warehouse_movement_export`
- **人员出勤**: 出勤统计表 `attendance_statistics`
- **退货管理**: 退货信息表 `return_info`
- **主数据**: 报价信息表 `quotation_info`、物料基本信息表 `material_basic_info`
- **资产管理**: 在账资产明细表 `fixed_asset_detail`、租赁资产库存导出表 `leased_asset_inventory`

## Goals / Non-Goals

**Goals:**
- 建立可编译运行的 Spring Boot 3 项目骨架
- 通过 `application.yml` + 环境变量配置 MySQL 只读数据源
- 配置 CORS 允许前端跨域访问
- 为 19 张表创建 Entity 实体类（含 MyBatis-Plus 注解）和 Mapper 接口
- 提供可用于 Docker 部署的多阶段构建 Dockerfile

**Non-Goals:**
- 不开发 Controller / Service / API 端点
- 不配置 SQLite 数据源
- 不编写单元测试
- 不创建 MyBatis XML Mapper 文件（Phase 2 按需添加）

## Decisions

### D1: Java 包结构

```
com.kejie.whop/
├── WhOpApplication.java          # 启动类
├── config/
│   ├── DataSourceConfig.java     # 数据源配置（可选，application.yml 足够）
│   ├── CorsConfig.java           # CORS 跨域配置
│   └── MyBatisPlusConfig.java    # MyBatis-Plus 配置（分页插件等）
├── model/entity/                 # 19 张表实体类
├── mapper/                       # 19 个 Mapper 接口
├── controller/                   # Phase 2 开发（本次为空包）
├── service/                      # Phase 2 开发（本次为空包）
└── util/                         # Phase 2 开发（本次为空包）
```

**理由**: 遵循 PROJECT_DESIGN.md 中定义的 `com.kejie.whop` 包路径；controller/service/util 预留空包但不放代码。

### D2: Maven 依赖选择

| 依赖 | 版本/说明 | 理由 |
|---|---|---|
| `spring-boot-starter-web` | 3.4.x | REST API 基础 |
| `mybatis-plus-spring-boot3-starter` | 3.5.9+ | MyBatis-Plus 对 Spring Boot 3 的支持 |
| `mysql-connector-j` | runtime | MySQL 5.7 驱动 |
| `lombok` | provided | 减少 Entity 样板代码 |
| `spring-boot-starter-validation` | — | 后续参数校验用 |

**理由**: MyBatis-Plus 使用 `mybatis-plus-spring-boot3-starter` 而非旧版 `mybatis-plus-boot-starter`，因为 Spring Boot 3 需要 Jakarta EE 命名空间。

### D3: MySQL 连接配置策略

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:10.126.50.199}:${MYSQL_PORT:3306}/${MYSQL_DB:wh_op_baseline}?useSSL=false&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      read-only: true
      maximum-pool-size: 10
      minimum-idle: 2
```

**理由**: 
- 使用 `${ENV_VAR:default}` 语法，本地开发可直接运行，Docker 部署通过环境变量覆盖
- `read-only: true` 确保不会意外修改源数据
- HikariCP 连接池默认配置足够当前规模

### D4: Entity 字段类型映射

| MySQL 类型 | Java 类型 | 说明 |
|---|---|---|
| `bigint(20) unsigned` | `Long` | 主键 `@TableId` |
| `varchar(n)` | `String` | — |
| `datetime` | `LocalDateTime` | — |
| `date` | `LocalDate` | — |
| `decimal(n,2)` | `BigDecimal` | — |
| `int(11)` | `Integer` | — |
| `text` | `String` | — |

Entity 类使用 Lombok `@Data` + MyBatis-Plus `@TableName` / `@TableId` / `@TableField` 注解。

### D5: CORS 配置

```java
// 允许所有来源、所有方法（第一版无认证，简化配置）
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
```

**理由**: 第一版无认证，简化 CORS 配置；生产环境应限制 `allowedOrigins`。

### D6: Dockerfile 多阶段构建

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**理由**: 多阶段构建减小镜像体积；先复制 pom.xml 利用 Docker 缓存加速依赖下载。

## Risks / Trade-offs

- **[MySQL 表名需确认]** → Entity 中 `@TableName` 使用 Profiling 报告推断的英文表名，首次启动时若报错需根据 `SHOW TABLES` 结果修正。可通过在 `application.yml` 中添加 MyBatis-Plus 的 `map-underscore-to-camel-case` 配置兼容
- **[大表查询性能]** → 仓位信息表（206万）、工作量统计操作明细表（212万）、复核操作表（204万）数据量大，本阶段不做分页/索引优化，Phase 7 专项处理
- **[MyBatis-Plus 版本兼容性]** → Spring Boot 3.4.x + MyBatis-Plus 3.5.9 版本需验证兼容，如有问题回退到 3.5.7
- **[字段数量多]** → 部分表字段 30+ 列，Entity 类会较长，使用 Lombok `@Data` 减少样板代码
