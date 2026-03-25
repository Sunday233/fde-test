## 1. 创建 Monorepo 根目录与 Git 初始化

- [x] 1.1 创建 `wh-op-platform/` 根目录
- [x] 1.2 在根目录执行 `git init` 初始化 Git 仓库
- [x] 1.3 创建子目录：`frontend/`、`backend/`、`analytics/`、`data/`、`docs/`
- [x] 1.4 在空子目录（frontend/、backend/、analytics/、data/）中创建 `.gitkeep` 文件

## 2. 创建 .gitignore

- [x] 2.1 在根目录创建 `.gitignore` 文件，包含以下分区：
  - Java/Maven 忽略规则（`target/`、`*.class`、`*.jar`、`*.war`、`.idea/`、`*.iml`）
  - Node.js 忽略规则（`node_modules/`、`dist/`、`.vite/`、`*.log`、`npm-debug.log*`）
  - Python 忽略规则（`__pycache__/`、`*.pyc`、`*.pyo`、`.venv/`、`venv/`、`env/`）
  - 项目通用规则（`.env`、`data/*.db`、`.DS_Store`）

## 3. 创建 .env.example

- [x] 3.1 在根目录创建 `.env.example`，包含 MySQL 连接变量（MYSQL_HOST、MYSQL_PORT、MYSQL_DB、MYSQL_USER、MYSQL_PASSWORD），敏感字段使用占位符
- [x] 3.2 添加服务配置变量（ANALYTICS_URL、SQLITE_PATH），附中文注释说明

## 4. 编写 README.md

- [x] 4.1 编写项目说明部分（项目名称、简介、核心用途）
- [x] 4.2 编写技术栈表格（前端/后端/分析服务/数据源/部署方式）
- [x] 4.3 编写目录结构树形说明
- [x] 4.4 编写快速启动指南（环境要求、.env 配置、docker compose 启动命令、各服务访问地址）

## 5. 迁移现有文档和脚本

- [x] 5.1 创建 `docs/data_profiling_reports/` 目录，复制 19 份 Profiling 报告
- [x] 5.2 创建 `docs/data_analysis_reports/` 目录，复制分析报告
- [x] 5.3 创建 `docs/scripts/` 目录，复制 `cost_analysis.py`、`warehouse_type_analysis.py`、`db_profile_script.py`

## 6. 验证

- [x] 6.1 验证目录结构完整性（所有子目录和文件都在正确位置）
- [x] 6.2 执行 `git add . && git status` 确认 .gitignore 规则生效（.env 不在暂存区、data/*.db 被忽略）
- [x] 6.3 执行首次 Git 提交：`git commit -m "chore: 项目初始化 - Monorepo 根目录结构"`
