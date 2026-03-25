## ADDED Requirements

### Requirement: Spring Boot 3 Maven 项目结构

系统 SHALL 在 `wh-op-platform/backend/` 下创建标准 Maven 项目结构，使用 Spring Boot 3.4.x 和 Java 21。

所属服务：backend

#### Scenario: pom.xml 包含必要依赖
- **WHEN** 查看 `backend/pom.xml`
- **THEN** SHALL 声明 `spring-boot-starter-parent` 3.4.x 作为 parent
- **THEN** SHALL 包含 `spring-boot-starter-web` 依赖
- **THEN** SHALL 包含 `mybatis-plus-spring-boot3-starter` 3.5.9+ 依赖
- **THEN** SHALL 包含 `mysql-connector-j` (runtime scope) 依赖
- **THEN** SHALL 包含 `lombok` (provided scope) 依赖
- **THEN** SHALL 包含 `spring-boot-starter-validation` 依赖
- **THEN** SHALL 设置 `java.version` 为 21
- **THEN** SHALL 设置 groupId 为 `com.kejie`，artifactId 为 `wh-op-platform`

#### Scenario: 启动类配置正确
- **WHEN** 查看 `WhOpApplication.java`
- **THEN** SHALL 位于 `com.kejie.whop` 包下
- **THEN** SHALL 使用 `@SpringBootApplication` 注解
- **THEN** SHALL 使用 `@MapperScan("com.kejie.whop.mapper")` 注解扫描 Mapper

#### Scenario: 目录结构完整
- **WHEN** 查看 backend 项目结构
- **THEN** SHALL 存在 `src/main/java/com/kejie/whop/` 目录
- **THEN** SHALL 存在 `config/`、`model/entity/`、`mapper/`、`controller/`、`service/` 子包
- **THEN** SHALL 存在 `src/main/resources/application.yml`

#### Scenario: 项目可编译
- **WHEN** 在 backend/ 目录执行 `mvn compile`
- **THEN** SHALL 编译成功无错误
