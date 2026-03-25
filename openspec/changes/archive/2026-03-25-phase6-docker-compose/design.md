## Context

项目已完成三层服务的独立开发：前端 Vue 3 + Nginx（:80）、后端 Spring Boot 3（:8080）、Python FastAPI 分析服务（:8000）。每个服务已有各自的 Dockerfile，前端 nginx.conf 已配置 `/api/` 转发到 `backend:8080`。`.env.example` 已定义环境变量模板。当前缺少将三者组合运行的 Docker Compose 编排文件。

现有文件清单：
- `frontend/Dockerfile` — Node 20 多阶段构建 + Nginx
- `frontend/nginx.conf` — 静态资源 + `/api/` 反向代理
- `backend/Dockerfile` — Maven 多阶段构建 + JRE 21
- `analytics/Dockerfile` — Python 3.11 slim + uvicorn
- `.env.example` — MySQL 连接、服务地址、SQLite 路径

服务间调用关系：
- 浏览器 → Nginx(:80) → `/api/` → Backend(:8080)
- Backend → `http://analytics:8000/api/...`（影响因素、基线、预计算结果）
- Analytics → MySQL(:3306) 只读查询 + SQLite(/data/results.db) 读写

## Goals / Non-Goals

**Goals:**
- 一键 `docker compose up --build` 启动完整三服务环境
- 环境变量统一通过 `.env` 文件注入，不硬编码敏感信息
- SQLite 数据通过 named volume 持久化，容器重启后不丢失
- README.md 提供清晰的部署说明

**Non-Goals:**
- 不涉及生产级编排（Kubernetes、Swarm）
- 不涉及 HTTPS / TLS 配置
- 不涉及 CI/CD 流水线
- 不涉及日志收集、APM 监控
- 不引入新的业务功能

## Decisions

### D1: Compose 文件版本与服务定义

采用 Docker Compose V2 格式（无需 `version` 字段），定义三个服务：`frontend`、`backend`、`analytics`。

**理由**：Docker Compose V2 是当前标准，`version` 字段已废弃。三个服务与现有目录结构一一对应。

### D2: 网络策略 — 使用默认 bridge 网络

不显式定义自定义网络，使用 Compose 自动创建的 `default` bridge 网络。三个服务通过服务名互相访问（`backend`、`analytics`）。

**理由**：单机部署场景下默认网络已足够。服务名即 DNS 名，nginx.conf 已使用 `backend:8080` 作为 upstream。

**替代方案**：自定义网络（如 `wh-net`）— 无实际收益，增加配置复杂度。

### D3: Volume 策略 — named volume 用于 SQLite 持久化

定义 named volume `analytics-data`，挂载到 analytics 容器的 `/data/` 目录。SQLite 文件 `/data/results.db` 存储在此 volume 中。

**理由**：named volume 由 Docker 管理，生命周期独立于容器。比 bind mount 更便携，适合预计算结果这种不需要开发者直接编辑的数据。

**替代方案**：bind mount `./data:/data` — 方便调试但依赖宿主机目录结构，跨环境不一致。

### D4: 环境变量注入方式

Compose 使用 `env_file: .env` 统一注入环境变量。backend 通过 Spring Boot `${VAR:default}` 语法读取，analytics 通过 pydantic `BaseSettings` 自动解析。

**理由**：单一 `.env` 文件管理所有配置，避免在 compose 文件中重复定义。两个服务已有环境变量读取机制。

### D5: 服务启动顺序

使用 `depends_on` 声明依赖：
- `backend` depends_on `analytics`（backend 需要调用 analytics API）
- `frontend` depends_on `backend`（nginx 反向代理目标是 backend）

不使用 `condition: service_healthy`，仅使用默认的 `service_started`。

**理由**：analytics 已有 HEALTHCHECK 指令，但 `service_healthy` 会增加启动等待时间。MVP 阶段 backend 的 AnalyticsClient 已有重试/降级逻辑，首次请求失败可接受。

### D6: 端口映射

仅暴露 frontend 的 80 端口到宿主机：`80:80`。backend 和 analytics 不映射宿主机端口（仅容器内网络可达）。

**理由**：用户只需访问 Nginx 入口。不暴露内部服务端口减少攻击面。开发调试时可临时添加端口映射。

### D7: backend 环境变量 — ANALYTICS_BASE_URL

backend 的 `application.yml` 使用 `${ANALYTICS_BASE_URL:http://localhost:8000}`。在 Docker 环境中需覆盖为 `http://analytics:8000`。通过 `.env` 中的 `ANALYTICS_BASE_URL=http://analytics:8000` 注入（`.env.example` 中已有此配置）。

### D8: README 部署说明结构

在现有 README.md 中新增「部署」章节，包含：前置条件、快速启动（3 步）、服务地址表、停止命令、常见问题。

## Risks / Trade-offs

- **[MySQL 网络可达性]** Docker 容器需要能访问远端 MySQL 10.126.50.199:3306。如果宿主机需要 VPN 或防火墙规则，容器内可能无法直连 → 缓解：使用 `network_mode: host` 或配置 Docker DNS。文档中注明此前提条件。
- **[SQLite 并发写入]** Analytics 服务是唯一的 SQLite 写入者，但如果未来扩展多实例会冲突 → 缓解：当前单实例部署，MVP 无此风险。
- **[构建时缓存]** Maven 和 npm 首次构建耗时较长（下载依赖）→ 缓解：Dockerfile 已利用多阶段构建和依赖层缓存。
- **[.env 泄露]** `.env` 包含数据库密码 → 缓解：确保 `.gitignore` 包含 `.env`（已有）。
