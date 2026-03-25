## Why

wh-op-platform 项目尚未初始化，需要创建 Monorepo 根目录结构、配置基础开发环境文件，并将已有的分析报告和脚本迁移到规范目录中，为后续 Phase 1-7 的开发打下基础。当前所有设计文档和分析脚本散落在临时目录，无法直接开始业务代码开发。

## What Changes

- 创建 Monorepo 根目录 `wh-op-platform/`，初始化 Git 仓库
- 编写根 `README.md`，包含项目说明、技术栈概述和快速启动指南
- 创建 `.gitignore`，覆盖 Java (Maven) / Node.js / Python 三种语言的忽略规则
- 创建 `.env.example` 模板，定义 MySQL 连接信息和服务配置项的环境变量
- 迁移现有数据分析报告（`data_profiling_reports/`、`data_analysis_reports/`）和分析脚本（`cost_analysis.py`、`warehouse_type_analysis.py`、`db_profile_script.py`）到 `docs/` 目录

## Capabilities

### New Capabilities
- `monorepo-scaffold`: Monorepo 根目录结构创建，包括 Git 初始化和子目录骨架（frontend/、backend/、analytics/、data/、docs/）
- `project-readme`: 项目 README.md 编写，涵盖项目说明、技术栈、目录结构、快速启动和部署指南
- `gitignore-config`: Java/Node.js/Python 混合项目的 .gitignore 规则配置
- `env-template`: .env.example 环境变量模板，定义 MySQL、Analytics 服务和 SQLite 路径等配置项
- `docs-migration`: 将现有分析报告和 Python 脚本迁移到 docs/ 规范目录

### Modified Capabilities

（无，这是全新项目初始化）

## Non-goals（非目标）

- 不涉及任何业务代码开发（Spring Boot / Vue / FastAPI）
- 不创建 Docker 相关文件（Dockerfile、docker-compose.yml 属于 Phase 6）
- 不配置 CI/CD 流水线
- 不安装任何依赖包（npm install / mvn / pip 属于各 Phase 骨架搭建任务）

## Impact

- **目录结构**: 创建 `wh-op-platform/` 根目录及 `frontend/`、`backend/`、`analytics/`、`data/`、`docs/` 子目录
- **文件**: 新增 README.md、.gitignore、.env.example
- **迁移**: `data_profiling_reports/`、`data_analysis_reports/`、`*.py` 脚本移入 `docs/`
- **涉及模块**: 项目根目录（不涉及 frontend / backend / analytics 的具体代码）
