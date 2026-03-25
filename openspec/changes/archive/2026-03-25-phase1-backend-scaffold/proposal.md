## Why

Phase 0 已完成 Monorepo 根目录初始化，但 `backend/` 目录仍为空。后续 Phase 2 的 API 开发、Phase 6 的 Docker 部署都依赖后端骨架的存在。需要尽快搭建 Spring Boot 3 项目结构，配置 MySQL 数据源连接和 CORS，创建 19 张表的 Entity/Mapper 层，使后端具备基本的数据访问能力。

所属服务：**backend**（Spring Boot 3）  
数据库操作：只读连接远端 MySQL（`wh_op_baseline`），不修改源数据

## What Changes

- 在 `wh-op-platform/backend/` 下创建 Spring Boot 3 项目骨架（Java 21, Maven, Spring Web, MyBatis-Plus）
- 配置 `application.yml`：MySQL 数据源（通过环境变量注入，连接远端 10.126.50.199:3306）
- 配置 CORS 跨域支持（允许前端 :80 访问后端 :8080）
- 创建 19 张 MySQL 表对应的 Entity 实体类和 MyBatis-Plus Mapper 接口
- 编写多阶段构建 Dockerfile

## Non-goals（非目标）

- 不开发任何 Controller / Service / API 端点（属于 Phase 2）
- 不配置 SQLite 数据源（属于 Phase 2/3 集成时处理）
- 不编写 AnalyticsClient（属于 Phase 2）
- 不编写单元测试（后续补充）
- 不配置 docker-compose.yml（属于 Phase 6）

## Capabilities

### New Capabilities
- `springboot-project-skeleton`: Spring Boot 3 项目骨架，包括 pom.xml、启动类、application.yml 配置
- `mysql-datasource-config`: MySQL 数据源配置（环境变量注入、只读连接、连接池）和 CORS 跨域配置
- `entity-mapper-layer`: 19 张表的 MyBatis-Plus Entity 实体类和 Mapper 接口定义
- `backend-dockerfile`: 后端多阶段构建 Dockerfile

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **目录**: `wh-op-platform/backend/` 从空目录变为完整的 Maven 项目
- **依赖**: Spring Boot 3.x、MyBatis-Plus、MySQL Connector、Lombok
- **数据库**: 只读连接 MySQL 5.7 @ 10.126.50.199:3306，库名 `wh_op_baseline`，19 张表
- **后续影响**: Phase 2 API 开发直接基于此骨架进行，Phase 6 Docker 部署依赖 Dockerfile
