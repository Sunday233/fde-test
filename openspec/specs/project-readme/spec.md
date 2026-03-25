## ADDED Requirements

### Requirement: 项目 README.md 编写

系统 SHALL 在 `wh-op-platform/` 根目录创建 `README.md` 文件，包含项目说明、技术栈概述、目录结构说明和快速启动指南。

所属服务：项目根目录

#### Scenario: README 包含项目说明
- **WHEN** 打开 README.md
- **THEN** SHALL 包含项目名称 "wh-op-platform — 科捷仓内操作费用基线分析平台"
- **THEN** SHALL 包含项目简介（精细化统计仓内操作费用的基线和影响因素）
- **THEN** SHALL 包含核心用途说明（估算新业务成本/报价、复盘旧业务、设定仓经理效率指标）

#### Scenario: README 包含技术栈说明
- **WHEN** 查看技术栈部分
- **THEN** SHALL 以表格形式列出前端（Vue 3 + Vite + TypeScript + Ant Design Vue + Tailwind CSS + ECharts）
- **THEN** SHALL 列出后端（Spring Boot 3 + Java 21 + Maven + MyBatis-Plus）
- **THEN** SHALL 列出分析服务（Python FastAPI）
- **THEN** SHALL 列出数据源（MySQL 5.7）和结果存储（SQLite）
- **THEN** SHALL 列出部署方式（Docker Compose）

#### Scenario: README 包含目录结构说明
- **WHEN** 查看目录结构部分
- **THEN** SHALL 以树形格式展示 Monorepo 的顶层目录结构及各目录用途

#### Scenario: README 包含快速启动指南
- **WHEN** 查看快速启动部分
- **THEN** SHALL 说明环境要求（Docker、Docker Compose）
- **THEN** SHALL 提供从 `.env.example` 复制 `.env` 并配置的步骤
- **THEN** SHALL 提供 `docker compose up --build` 一键启动命令
- **THEN** SHALL 说明各服务的访问地址（前端 :80、后端 :8080、分析服务 :8000）
