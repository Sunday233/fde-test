## ADDED Requirements

### Requirement: 创建 Monorepo 根目录结构

系统 SHALL 创建 `wh-op-platform/` 根目录，并在其中初始化 Git 仓库。根目录下 SHALL 包含以下子目录：`frontend/`、`backend/`、`analytics/`、`data/`、`docs/`。

所属服务：项目根目录（不涉及具体服务）

#### Scenario: 初始化 Git 仓库
- **WHEN** 执行 `git init` 命令
- **THEN** `wh-op-platform/` 下 SHALL 生成 `.git/` 目录，仓库初始化成功

#### Scenario: 子目录结构创建
- **WHEN** 项目初始化完成
- **THEN** 根目录下 SHALL 存在以下目录：`frontend/`、`backend/`、`analytics/`、`data/`、`docs/`
- **THEN** 每个空目录 SHALL 包含 `.gitkeep` 文件以确保 Git 可追踪

#### Scenario: 子目录用途标识
- **WHEN** 查看各子目录
- **THEN** `frontend/` 用于 Vue 3 前端项目
- **THEN** `backend/` 用于 Spring Boot 3 后端项目
- **THEN** `analytics/` 用于 Python FastAPI 分析服务
- **THEN** `data/` 用于 SQLite 预计算结果存储（被 .gitignore 忽略）
- **THEN** `docs/` 用于存放文档、分析报告和脚本
