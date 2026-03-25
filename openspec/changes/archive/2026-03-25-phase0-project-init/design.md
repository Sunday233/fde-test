## Context

wh-op-platform 是一个 Monorepo 前后端分离 Web 应用，包含三个服务：Vue 3 前端、Spring Boot 3 后端和 Python FastAPI 分析服务。当前项目处于零基础状态，需要先完成根目录初始化，才能进入各服务模块的搭建（Phase 1-4）。

现有资产包括：
- 19 张表的数据 Profiling 报告（`data_profiling_reports/`）
- 数据分析报告 2 份（`data_analysis_reports/`）
- Python 分析脚本 3 个（`cost_analysis.py`、`warehouse_type_analysis.py`、`db_profile_script.py`）

这些资产需要迁移到 Monorepo 的 `docs/` 目录下保存。

## Goals / Non-Goals

**Goals:**
- 建立一个规范的 Monorepo 根目录结构，包含 `frontend/`、`backend/`、`analytics/`、`data/`、`docs/` 五个子目录
- 提供清晰的 README.md 让开发者理解项目全貌和快速启动方式
- 通过 .gitignore 正确忽略 Java/Node.js/Python 三种语言的构建产物和依赖目录
- 通过 .env.example 模板明确所有需要配置的环境变量，避免敏感信息硬编码
- 将现有分析资产迁移到规范位置

**Non-Goals:**
- 不创建 Docker 相关文件（Dockerfile、docker-compose.yml）
- 不安装任何语言依赖
- 不编写任何业务代码
- 不配置 CI/CD

## Decisions

### D1: Monorepo 目录结构

采用 PROJECT_DESIGN.md 中定义的扁平 Monorepo 结构：

```
wh-op-platform/
├── README.md
├── .gitignore
├── .env.example
├── frontend/          # Vue 3 (Phase 4 搭建)
├── backend/           # Spring Boot 3 (Phase 1 搭建)
├── analytics/         # Python FastAPI (Phase 3 搭建)
├── data/              # SQLite 结果存储 (Git 忽略)
└── docs/              # 文档和报告
    ├── data_profiling_reports/
    ├── data_analysis_reports/
    └── scripts/       # 迁移的分析脚本
```

**理由**: 扁平结构比 Lerna/Nx 等工具驱动的 Monorepo 更简单，三个服务技术栈完全不同（Java/Node/Python），无需统一构建工具链。

### D2: 环境变量管理

使用 `.env.example` 模板 + `.env` 实际配置（被 .gitignore 忽略）的方式管理敏感信息。

环境变量包括：
| 变量名 | 说明 | 示例值 |
|---|---|---|
| `MYSQL_HOST` | MySQL 主机地址 | `10.126.50.199` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DB` | 数据库名 | `wh_op_baseline` |
| `MYSQL_USER` | 数据库用户名 | `fdeuser` |
| `MYSQL_PASSWORD` | 数据库密码 | `(请填写)` |
| `ANALYTICS_URL` | Python 分析服务地址 | `http://localhost:8000` |
| `SQLITE_PATH` | SQLite 存储路径 | `./data/results.db` |

**理由**: 避免在 docker-compose.yml 或 application.yml 中硬编码密码；.env.example 既是文档又是模板。

### D3: .gitignore 策略

合并三种语言的忽略规则到一个文件，分区块标注：

- **Java/Maven**: `target/`、`.class`、`*.jar`、`.idea/`
- **Node.js**: `node_modules/`、`dist/`、`.vite/`
- **Python**: `__pycache__/`、`*.pyc`、`.venv/`、`venv/`
- **项目通用**: `.env`、`data/*.db`、`.DS_Store`

**理由**: 单文件统一管理比分散在各子目录更直观，且减少遗漏风险。

### D4: 文档迁移方式

迁移策略为**复制**（而非移动），保留原始位置的文件不变，避免影响当前正在使用的引用。

- `data_profiling_reports/` → `docs/data_profiling_reports/`
- `data_analysis_reports/` → `docs/data_analysis_reports/`
- `cost_analysis.py`、`warehouse_type_analysis.py`、`db_profile_script.py` → `docs/scripts/`

**理由**: 初始化阶段保守操作，后续在 Monorepo 完全搭建后再清理原始文件。

## Risks / Trade-offs

- **[子目录为空无法 Git 跟踪]** → 在空目录（frontend/、backend/、analytics/、data/）中放置 `.gitkeep` 文件
- **[.env.example 可能遗漏变量]** → 在 README 中说明各服务的完整配置参考各自的 application.yml / config.py
- **[迁移后文档路径变化]** → README 中明确标注文档位置，脚本中的相对路径引用需要在后续 Phase 中调整
