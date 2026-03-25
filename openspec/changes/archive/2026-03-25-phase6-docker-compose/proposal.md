## Why

项目的三个服务（前端 Vue/Nginx、后端 Spring Boot、Python 分析服务）各自已有 Dockerfile，但缺乏统一编排。开发者无法一键启动完整环境，也没有标准的环境变量管理和持久化策略。需要 Docker Compose 将三者组合为可复现的本地部署方案。

## What Changes

- 新建 `docker-compose.yml`，定义 frontend（Nginx :80）、backend（Spring Boot :8080）、analytics（FastAPI :8000）三个服务，以及 SQLite 数据持久化 volume
- 基于 `.env.example` 创建实际 `.env` 文件模板，管理 MySQL 连接、服务间通信地址等敏感配置
- 验证 Nginx 反向代理将 `/api/` 请求正确转发至 backend，backend 通过 Docker 内部网络调用 analytics
- 验证 SQLite volume 挂载，确保容器重启后预计算结果不丢失
- 补充 `README.md` 中的部署说明（前置条件、启动命令、服务地址）

### 非目标

- 不涉及 CI/CD 流水线配置
- 不涉及生产环境 Kubernetes / Swarm 编排
- 不涉及 HTTPS / TLS 证书配置
- 不涉及业务逻辑变更

## Capabilities

### New Capabilities

- `docker-compose-orchestration`: Docker Compose 编排文件，定义 3 个服务的构建、网络、依赖、环境变量和 volume 配置
- `deployment-env-config`: 环境变量管理 — `.env` 文件策略与敏感配置注入
- `deployment-docs`: README.md 部署说明章节 — 前置条件、快速启动、服务地址、常见问题

### Modified Capabilities

- `frontend-dockerfile`: nginx.conf 可能需要调整以确保 Docker 网络内反向代理正确（当前已配置 `proxy_pass http://backend:8080`，需验证是否满足）

## Impact

- **新文件**: `wh-op-platform/docker-compose.yml`、`wh-op-platform/.env`（gitignored）
- **修改文件**: `wh-op-platform/README.md`（新增部署说明章节）
- **依赖**: Docker Engine 20+、Docker Compose v2
- **网络**: 三个服务共享 Docker bridge 网络，frontend → backend → analytics 链路
- **存储**: named volume `analytics-data` 挂载到 analytics 容器的 `/data/` 目录
