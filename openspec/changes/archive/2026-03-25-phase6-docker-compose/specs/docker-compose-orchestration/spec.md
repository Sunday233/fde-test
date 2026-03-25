# docker-compose-orchestration

**所属服务**: 跨服务（frontend + backend + analytics）

## ADDED Requirements

### Requirement: Docker Compose 编排文件

系统 SHALL 在 `wh-op-platform/docker-compose.yml` 中定义三个服务和一个 named volume：

**服务定义**:
- `frontend`: 基于 `frontend/Dockerfile` 构建，映射 `80:80`，依赖 `backend`
- `backend`: 基于 `backend/Dockerfile` 构建，不映射宿主机端口，依赖 `analytics`，通过 `env_file` 注入环境变量
- `analytics`: 基于 `analytics/Dockerfile` 构建，不映射宿主机端口，挂载 `analytics-data:/data`，通过 `env_file` 注入环境变量

**Volume 定义**:
- `analytics-data`: named volume，挂载到 analytics 容器 `/data/` 目录，持久化 SQLite 预计算结果

三个服务 SHALL 共享 Compose 默认 bridge 网络，通过服务名互相访问。

#### Scenario: docker-compose.yml 语法有效

- **WHEN** 在 `wh-op-platform/` 目录执行 `docker compose config`
- **THEN** 命令成功输出解析后的 Compose 配置，无语法错误

#### Scenario: 三服务完整启动

- **WHEN** 执行 `docker compose up --build -d`
- **THEN** 三个容器均进入 running 状态，`docker compose ps` 显示 frontend(80)、backend(8080)、analytics(8000)

### Requirement: 服务构建上下文配置

每个服务的 `build.context` SHALL 指向对应的子目录：
- frontend: `context: ./frontend`
- backend: `context: ./backend`
- analytics: `context: ./analytics`

Dockerfile 路径使用默认（`Dockerfile`），无需指定 `dockerfile` 字段。

#### Scenario: 各服务独立构建

- **WHEN** 执行 `docker compose build frontend`
- **THEN** Docker 在 `frontend/` 目录上下文中执行多阶段构建，产出 Nginx 镜像

### Requirement: 服务启动依赖

系统 SHALL 使用 `depends_on` 声明启动顺序：
- `frontend` depends_on `backend`
- `backend` depends_on `analytics`

启动顺序 SHALL 为：analytics → backend → frontend。

#### Scenario: 依赖服务先启动

- **WHEN** 执行 `docker compose up`
- **THEN** Docker Compose 按 analytics → backend → frontend 顺序启动容器

### Requirement: 端口映射策略

仅 `frontend` 服务 SHALL 映射宿主机端口 `80:80`。`backend` 和 `analytics` 服务不映射宿主机端口，仅在容器网络内可达。

#### Scenario: 宿主机仅暴露前端端口

- **WHEN** 三个服务全部启动
- **THEN** 宿主机 `http://localhost:80` 可访问前端页面；宿主机 `localhost:8080` 和 `localhost:8000` 不可达

### Requirement: Volume 持久化

`analytics-data` named volume SHALL 在容器删除后保留数据。

#### Scenario: 容器重建后数据保留

- **WHEN** 执行 `docker compose down` 后再执行 `docker compose up`
- **THEN** analytics 容器中的 `/data/results.db` 文件内容与之前一致，预计算结果不丢失

#### Scenario: 完全清理

- **WHEN** 执行 `docker compose down -v`
- **THEN** `analytics-data` volume 被删除，下次启动时 analytics 服务从空数据库开始，重新触发预计算

### Requirement: 环境变量注入

三个服务 SHALL 通过 `env_file: .env` 注入环境变量。所需变量包括：
- MySQL 连接：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DB`、`MYSQL_USER`、`MYSQL_PASSWORD`
- 服务间通信：`ANALYTICS_BASE_URL`（backend 用，值为 `http://analytics:8000`）
- 存储路径：`SQLITE_PATH`（analytics 用，值为 `/data/results.db`）

#### Scenario: 环境变量正确传递

- **WHEN** `.env` 中设置 `MYSQL_HOST=10.126.50.199`
- **THEN** backend 容器内 `echo $MYSQL_HOST` 输出 `10.126.50.199`，Spring Boot 使用此值连接 MySQL
