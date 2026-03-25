# deployment-docs

**所属服务**: 项目级文档

## ADDED Requirements

### Requirement: README 部署章节

系统 SHALL 在 `wh-op-platform/README.md` 中新增「部署」章节，包含以下子节：

1. **前置条件** — Docker Engine 20+、Docker Compose v2、可访问 MySQL 10.126.50.199:3306 的网络
2. **快速启动** — 3 步操作：复制 .env → 填写密码 → docker compose up --build
3. **服务地址** — 表格列出服务名、容器端口、宿主机访问方式
4. **停止与清理** — `docker compose down` 和 `docker compose down -v` 的区别
5. **常见问题** — MySQL 连接失败、端口冲突等问题的排查指引

#### Scenario: 新开发者按文档部署成功

- **WHEN** 新开发者按 README 部署章节的步骤操作
- **THEN** 三个服务成功启动，浏览器访问 `http://localhost` 显示 Dashboard 页面

#### Scenario: 文档包含服务地址表

- **WHEN** 阅读部署章节的「服务地址」子节
- **THEN** 能看到表格包含：frontend(http://localhost:80)、backend(容器内 8080，不对外暴露)、analytics(容器内 8000，不对外暴露)

### Requirement: 快速启动步骤

快速启动 SHALL 包含且仅包含以下 3 步：
1. `cp .env.example .env` 并编辑 `.env` 填写实际 MySQL 用户名和密码
2. `docker compose up --build -d`
3. 访问 `http://localhost`

#### Scenario: 最小化启动步骤

- **WHEN** 从零开始部署
- **THEN** 只需 3 个命令/操作即可完成部署，无需额外安装依赖或编译

### Requirement: 常见问题排查

README SHALL 包含以下常见问题及解决方案：
- MySQL 连接超时（检查网络、防火墙、VPN）
- 端口 80 被占用（修改 docker-compose.yml 端口映射或停止占用端口的进程）
- 首次启动慢（Maven/npm 依赖下载，后续构建有缓存）

#### Scenario: MySQL 连接失败排查

- **WHEN** backend 日志显示 MySQL 连接超时
- **THEN** README 常见问题提供检查网络连通性的命令和排查步骤
