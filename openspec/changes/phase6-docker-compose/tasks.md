## 1. Docker Compose 编排文件

- [x] 1.1 创建 `wh-op-platform/docker-compose.yml`，定义 `frontend`、`backend`、`analytics` 三个服务，配置 `build.context` 指向各子目录，声明 `depends_on` 启动依赖（analytics → backend → frontend）
- [x] 1.2 配置 `analytics-data` named volume，挂载到 analytics 容器 `/data/` 目录；仅 frontend 映射宿主机端口 `80:80`
- [x] 1.3 为 backend 和 analytics 服务配置 `env_file: .env`，确保环境变量正确注入

## 2. 环境变量配置

- [x] 2.1 基于 `.env.example` 创建 `.env` 文件，填写实际 MySQL 连接信息、`ANALYTICS_BASE_URL=http://analytics:8000`、`SQLITE_PATH=/data/results.db`
- [x] 2.2 验证 `.gitignore` 已包含 `.env` 规则，确保敏感信息不提交到版本控制
- [x] 2.3 同步更新 `.env.example`，确保变量名与 `.env` 完全一致（值使用占位符）

## 3. Nginx 反向代理验证

- [x] 3.1 检查 `frontend/nginx.conf` 中 `proxy_pass http://backend:8080` 配置，确认在 Docker Compose 网络中可正确解析 `backend` 主机名

## 4. 服务通信验证

- [x] 4.1 启动三服务（`docker compose up --build -d`），验证 frontend → backend（浏览器访问 `http://localhost/api/warehouses` 返回数据）
- [x] 4.2 验证 backend → analytics 通信（访问需要 Python 分析结果的 API，如 `/api/impact/factors`，确认返回正常）

## 5. Volume 持久化验证

- [x] 5.1 验证 analytics 容器内 `/data/results.db` 存在且有数据；执行 `docker compose down` + `docker compose up -d`，确认数据未丢失
- [x] 5.2 验证 `docker compose down -v` 后 volume 被清除，重启后 analytics 重新触发预计算

## 6. README 部署说明

- [x] 6.1 在 `wh-op-platform/README.md` 新增「部署」章节，包含前置条件（Docker 20+、Compose v2、MySQL 网络可达）
- [x] 6.2 编写快速启动步骤（3 步：复制 .env → 填写密码 → docker compose up --build）、服务地址表、停止/清理命令
- [x] 6.3 编写常见问题排查（MySQL 连接超时、端口 80 冲突、首次构建慢）
