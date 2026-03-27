# wh-op-platform — 科捷仓内操作费用基线分析平台

精细化统计仓内操作费用的基线和影响因素，建立费用基线的相关算数应用。

**核心用途：**
- 估算新业务成本和报价
- 复盘旧业务运营效率
- 设定仓经理效率指标

**费用定义：** 操作费用 = 工时数 × 劳务单价（含固定劳务、长期劳务外包、临时劳务外包）

---

## 技术栈

| 层级 | 技术栈 | 版本/说明 |
|---|---|---|
| **前端** | Vue 3 + Vite | TypeScript, SFC |
| **UI 框架** | Ant Design Vue + Tailwind CSS | 4.x |
| **图表** | ECharts | vue-echarts 封装 |
| **后端** | Spring Boot 3 | Java 21, Maven |
| **ORM** | MyBatis-Plus | 适合复杂统计查询 |
| **数据分析服务** | Python FastAPI | 独立微服务 |
| **数据源** | MySQL 5.7 | 远端直连，只读 |
| **结果存储** | SQLite | Docker 内轻量存储预计算结果 |
| **部署** | Docker Compose | 单机部署 |

---

## 目录结构

```
wh-op-platform/
├── README.md                   # 项目说明
├── .gitignore                  # Git 忽略规则
├── .env.example                # 环境变量模板
├── docker-compose.yml          # Docker 编排（Phase 6 创建）
│
├── frontend/                   # Vue 3 前端（Nginx :80）
├── backend/                    # Spring Boot 3 后端（:3000）
├── analytics/                  # Python FastAPI 分析服务（:8000）
├── data/                       # SQLite 预计算结果（Git 忽略）
└── docs/                       # 文档
    ├── data_profiling_reports/ # 19 张表的数据 Profiling 报告
    ├── data_analysis_reports/  # 数据分析报告
    └── scripts/                # 分析脚本（已有 Python 脚本）
```

---

## 本地开发启动

### 前置条件（本地）

| 工具 | 版本要求 | 安装方式 |
|---|---|---|
| Java (JDK) | 17+ (推荐 OpenJDK 25) | `brew install openjdk` |
| Maven | 3.8+ | `brew install maven` |
| Node.js | 20.x | `nvm install 20` |
| Python | 3.9+ | 系统自带或 `brew install python` |
| pip 包 | uvicorn, fastapi 等 | 见 `analytics/requirements.txt` |

> macOS 注意：`/usr/bin/java` 是系统存根，实际运行需通过 Homebrew 路径，推荐直接用 `mvn spring-boot:run`（Maven 会自动使用 `JAVA_HOME`）。

### 1. 启动分析服务（Python FastAPI · 端口 8000）

```bash
cd analytics
python3 -m uvicorn src.main:app --host 0.0.0.0 --port 8000 --reload
```

首次运行会自动初始化 SQLite 数据库（`analytics/data/results.db`）并执行预计算。

### 2. 启动后端（Spring Boot · 端口 3000）

```bash
cd backend
export MYSQL_USER=fdeuser
export MYSQL_PASSWORD=your_password
export ANALYTICS_BASE_URL=http://localhost:8000
mvn spring-boot:run
```

等待日志出现 `Started WhOpApplication` 即启动完成。

### 3. 启动前端（Vite Dev Server · 端口 5173）

```bash
cd frontend
nvm use 20         # 确保 Node >= 20.19，避免 Vite 因 Node 16 启动失败
npm install          # 首次运行需安装依赖
npm run dev
```

Vite 固定监听 `http://localhost:5173`（`strictPort=true`），并自动将 `/api/*` 代理到后端 `http://localhost:3000`。

### 服务地址（本地开发）

| 服务 | 地址 | 说明 |
|---|---|---|
| 前端页面 | http://localhost:5173 | Vite HMR 热更新 |
| 后端 API | http://localhost:3000/api | Spring Boot REST |
| 分析服务 | http://localhost:8000 | Python FastAPI |

---

## 生产部署（Docker）

### 前置条件

- [Docker Engine](https://www.docker.com/) >= 20.10
- [Docker Compose](https://docs.docker.com/compose/) v2（`docker compose` 命令）
- 网络可访问 MySQL 服务器 `10.126.50.199:3306`（如需 VPN 请确保 Docker 也在 VPN 网络内）

### 快速启动

```bash
# 1. 复制环境变量模板并填写实际 MySQL 用户名和密码
cp .env.example .env
# 编辑 .env，修改 MYSQL_USER 和 MYSQL_PASSWORD

# 2. 一键构建并启动
docker compose up --build -d

# 3. 访问应用
open http://localhost
```

### 服务地址

| 服务 | 容器端口 | 宿主机访问 | 说明 |
|---|---|---|---|
| 前端 | 80 | http://localhost | Vue 3 + Nginx 反向代理 |
| 后端 API | 3000 | 不对外暴露（通过 Nginx `/api/` 转发） | Spring Boot RESTful API |
| 分析服务 | 8000 | 不对外暴露（Backend 内部调用） | Python FastAPI |

> 后端和分析服务仅在 Docker 内部网络可达，所有 API 请求统一通过 Nginx 端口 80 的 `/api/` 路径转发。

### 停止与清理

```bash
# 停止服务（保留数据）
docker compose down

# 停止服务并清除 SQLite 预计算数据（下次启动会重新计算）
docker compose down -v
```

### 常见问题

**Q: MySQL 连接超时 / 无法连接**

检查宿主机能否访问 MySQL：
```bash
nc -zv 10.126.50.199 3306
```
如果宿主机通过 VPN 访问，确保 Docker 容器也在同一网络。可尝试在 `docker-compose.yml` 中为 backend 和 analytics 添加 `network_mode: host`。

**Q: 端口 80 被占用**

修改 `docker-compose.yml` 中 frontend 的端口映射：
```yaml
ports:
  - "8888:80"  # 改为其他可用端口
```
然后访问 `http://localhost:8888`。

**Q: 首次构建很慢**

Maven（后端）和 npm（前端）首次构建需要下载依赖，可能耗时较长。后续构建有 Docker 层缓存，速度会显著提升。

> 各服务的详细配置请参考 `backend/src/main/resources/application.yml`（后端）和 `analytics/src/config.py`（分析服务）。
