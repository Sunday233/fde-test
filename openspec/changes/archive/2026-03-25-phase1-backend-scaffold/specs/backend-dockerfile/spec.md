## ADDED Requirements

### Requirement: 后端多阶段构建 Dockerfile

系统 SHALL 在 `wh-op-platform/backend/` 下创建 Dockerfile，使用多阶段构建减小最终镜像体积。

所属服务：backend

#### Scenario: 构建阶段
- **WHEN** 查看 Dockerfile 的构建阶段
- **THEN** SHALL 使用 `maven:3.9-eclipse-temurin-21` 作为构建基础镜像
- **THEN** SHALL 先复制 `pom.xml` 并执行 `mvn dependency:go-offline` 利用 Docker 缓存
- **THEN** SHALL 再复制源码并执行 `mvn package -DskipTests`

#### Scenario: 运行阶段
- **WHEN** 查看 Dockerfile 的运行阶段
- **THEN** SHALL 使用 `eclipse-temurin:21-jre` 作为运行基础镜像
- **THEN** SHALL 从构建阶段复制 `target/*.jar` 到运行镜像
- **THEN** SHALL 暴露端口 `8080`
- **THEN** ENTRYPOINT SHALL 为 `["java", "-jar", "app.jar"]`

#### Scenario: 镜像可构建
- **WHEN** 在 backend/ 目录执行 `docker build -t wh-op-backend .`
- **THEN** SHALL 构建成功
