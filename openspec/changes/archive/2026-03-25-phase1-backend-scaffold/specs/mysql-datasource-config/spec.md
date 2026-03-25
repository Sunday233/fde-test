## ADDED Requirements

### Requirement: MySQL 数据源配置

系统 SHALL 在 `application.yml` 中配置 MySQL 数据源，通过环境变量注入连接信息，使用 HikariCP 连接池并设置为只读模式。

所属服务：backend  
数据库操作：只读 MySQL（远端 10.126.50.199:3306，库名 `wh_op_baseline`）

#### Scenario: 数据源通过环境变量配置
- **WHEN** 查看 `application.yml` 的 datasource 配置
- **THEN** url SHALL 使用 `${MYSQL_HOST:10.126.50.199}`、`${MYSQL_PORT:3306}`、`${MYSQL_DB:wh_op_baseline}` 环境变量并带默认值
- **THEN** username SHALL 使用 `${MYSQL_USER}` 环境变量
- **THEN** password SHALL 使用 `${MYSQL_PASSWORD}` 环境变量
- **THEN** driver-class-name SHALL 为 `com.mysql.cj.jdbc.Driver`

#### Scenario: 连接池配置
- **WHEN** 查看 HikariCP 配置
- **THEN** `read-only` SHALL 为 `true`
- **THEN** `maximum-pool-size` SHALL 不超过 10
- **THEN** `minimum-idle` SHALL 为 2

#### Scenario: 字符编码和时区
- **WHEN** 查看 JDBC URL 参数
- **THEN** SHALL 包含 `characterEncoding=utf8mb4`
- **THEN** SHALL 包含 `serverTimezone=Asia/Shanghai`
- **THEN** SHALL 包含 `useSSL=false`

### Requirement: CORS 跨域配置

系统 SHALL 配置 CORS 允许前端跨域访问后端 API。

所属服务：backend

#### Scenario: CORS 规则覆盖 API 路径
- **WHEN** 前端从 `:80` 发起请求到后端 `:8080` 的 `/api/**` 路径
- **THEN** CorsConfig SHALL 允许所有来源 (`allowedOriginPatterns("*")`)
- **THEN** SHALL 允许所有 HTTP 方法 (`GET, POST, PUT, DELETE, OPTIONS`)
- **THEN** SHALL 允许所有请求头

### Requirement: MyBatis-Plus 配置

系统 SHALL 配置 MyBatis-Plus 的分页插件和驼峰命名映射。

所属服务：backend

#### Scenario: MyBatis-Plus 全局配置
- **WHEN** 查看 `application.yml` 的 mybatis-plus 配置
- **THEN** SHALL 启用 `map-underscore-to-camel-case`（下划线转驼峰）
- **THEN** SHALL 配置 `id-type` 为 `auto`（主键自增）
- **THEN** SHALL 配置 `table-prefix` 为空（无前缀）

#### Scenario: 分页插件注册
- **WHEN** 查看 MyBatisPlusConfig 配置类
- **THEN** SHALL 注册 `MybatisPlusInterceptor` 并添加 `PaginationInnerInterceptor`
- **THEN** 分页插件 SHALL 指定 `DbType.MYSQL`
