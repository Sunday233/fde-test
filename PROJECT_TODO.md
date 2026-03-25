# wh-op-platform — 项目待办事项 (TODO)

**项目**: 科捷仓内操作费用基线分析平台  
**创建日期**: 2026-03-25  

---

## Phase 0: 项目初始化

- [ ] **T0.1** 创建 Monorepo 根目录 `wh-op-platform/`，初始化 Git
- [ ] **T0.2** 编写根 `README.md`（项目说明、快速启动指南）
- [ ] **T0.3** 创建 `.gitignore`（Java/Node/Python 混合项目规则）
- [ ] **T0.4** 创建 `.env.example` 模板（MySQL 连接信息等）
- [ ] **T0.5** 迁移现有分析报告和脚本到 `docs/` 目录

---

## Phase 1: 后端 — Spring Boot 3 项目搭建

- [ ] **T1.1** 使用 Spring Initializr 创建项目骨架（Java 21, Maven, Spring Web, MyBatis-Plus）
- [ ] **T1.2** 配置 MySQL 数据源（`application.yml`，连接远端 10.126.50.199）
- [ ] **T1.3** 配置跨域 CORS
- [ ] **T1.4** 创建 19 张表对应的 Entity/Mapper（MyBatis-Plus 代码生成或手写）
  - 出库单表、入库单表、入库单行明细表
  - 出勤统计表、报价信息表
  - 上架单表、上架单明细表
  - 拣货操作表、拣货操作明细表
  - 复核操作表
  - 工作量统计信息表、工作量统计操作明细表
  - 退货信息表
  - 仓位信息表、仓位库存信息表
  - 物料基本信息表
  - 库内移动导出表
  - 在账资产明细表、租赁资产库存导出表
- [ ] **T1.5** 编写 Dockerfile（多阶段构建）

---

## Phase 2: 后端 — API 开发

### Dashboard API
- [ ] **T2.1** `GET /api/dashboard/overview` — 核心 KPI 概览
- [ ] **T2.2** `GET /api/dashboard/trend` — 多维趋势数据

### 费用基线 API
- [ ] **T2.3** `GET /api/baseline/monthly` — 月度费用基线汇总
- [ ] **T2.4** `GET /api/baseline/warehouse/{id}` — 单仓详情
- [ ] **T2.5** `GET /api/baseline/compare` — 双仓/多仓对比

### 成本估算 API
- [ ] **T2.6** `POST /api/estimate/calculate` — 费用估算计算（公式硬编码）
- [ ] **T2.7** `GET /api/estimate/defaults/{warehouseId}` — 历史默认参数

### 影响因素 API（代理 Python 结果）
- [ ] **T2.8** `GET /api/impact/factors` — 影响因素排序
- [ ] **T2.9** `GET /api/impact/correlation` — 相关性矩阵

### 报告 API
- [ ] **T2.10** `POST /api/report/generate` — 生成报告
- [ ] **T2.11** `GET /api/report/list` — 报告列表
- [ ] **T2.12** `GET /api/report/{id}` — 报告内容

### 通用 API
- [ ] **T2.13** `GET /api/warehouses` — 仓库列表

### Python 服务调用
- [ ] **T2.14** 实现 `AnalyticsClient.java`（HTTP 调用 FastAPI 服务）

---

## Phase 3: Python 分析服务 — FastAPI

- [ ] **T3.1** FastAPI 项目骨架搭建（pyproject.toml / requirements.txt）
- [ ] **T3.2** MySQL 连接模块（pymysql / SQLAlchemy）
- [ ] **T3.3** SQLite 结果存储模块
- [ ] **T3.4** 健康检查 API（`GET /api/health`）
- [ ] **T3.5** **费用基线分析服务**
  - 从 MySQL 读取出库、入库、出勤、报价等数据
  - 日粒度 × 仓库维度汇总
  - 月度费用基线计算
  - 结果写入 SQLite `baseline_results` 表
- [ ] **T3.6** **影响因素分析服务**
  - Pearson 相关系数计算
  - 因素重要性排序
  - 结果写入 SQLite `impact_results` 表
- [ ] **T3.7** **日维度明细计算**
  - 聚合日维度指标（出库、入库、退货、出勤等）
  - 结果写入 SQLite `daily_metrics` 表
- [ ] **T3.8** 预计算调度器（启动时 + 定时触发）
- [ ] **T3.9** 迁移现有 `cost_analysis.py` 和 `warehouse_type_analysis.py` 的核心逻辑
- [ ] **T3.10** 编写 Dockerfile

---

## Phase 4: 前端 — Vue 3 项目搭建

- [ ] **T4.1** Vite + Vue 3 + TypeScript 项目初始化
- [ ] **T4.2** 安装配置 Ant Design Vue 4.x + Tailwind.css
- [ ] **T4.3** 安装配置 vue-echarts + echarts
- [ ] **T4.4** 安装配置 Vue Router、Pinia、Axios
- [ ] **T4.5** 创建主布局 `MainLayout.vue`（左侧菜单 + 顶栏 + 内容区）
- [ ] **T4.6** 配置路由（Dashboard / 费用基线 / 影响因素 / 成本估算 / 报告）
- [ ] **T4.7** API 封装层（`src/api/index.ts`）
- [ ] **T4.8** TypeScript 类型定义
- [ ] **T4.9** 编写 Dockerfile（多阶段构建 + Nginx）
- [ ] **T4.10** 编写 `nginx.conf`（静态资源 + API 反向代理）

---

## Phase 5: 前端 — 页面开发

### Dashboard 页面
- [ ] **T5.1** KPI 卡片组件（总单量、总工时、月度费用、人效）
- [ ] **T5.2** 日出库单量趋势折线图（按仓库）
- [ ] **T5.3** 月度费用构成柱状图
- [ ] **T5.4** 操作类型工作量分布图

### 费用基线分析页面
- [ ] **T5.5** 筛选面板（仓库、月份、费用类型）
- [ ] **T5.6** 月度基线数据表格
- [ ] **T5.7** 劳务单价对比图
- [ ] **T5.8** 双仓月度对比图

### 影响因素分析页面
- [ ] **T5.9** 影响因素重要性排序柱状图
- [ ] **T5.10** Pearson 相关系数热力图
- [ ] **T5.11** 单因素散点图（可选因素 vs 工时）
- [ ] **T5.12** 双仓因素对比

### 成本估算/报价模拟页面
- [ ] **T5.13** 参数输入表单（单量、件单比、人效、天数等）
- [ ] **T5.14** 实时计算结果展示（人数、工时、费用、单均/件均成本）
- [ ] **T5.15** 与历史数据对比

### 报告页面
- [ ] **T5.16** 报告列表（已生成报告）
- [ ] **T5.17** 报告生成表单（选仓库、时间范围）
- [ ] **T5.18** Markdown 在线渲染预览
- [ ] **T5.19** 报告下载（HTML 格式）

---

## Phase 6: Docker Compose 集成与部署

- [ ] **T6.1** 编写 `docker-compose.yml`（3 个服务 + volume）
- [ ] **T6.2** 创建 `.env` 文件管理敏感配置
- [ ] **T6.3** 前端 Nginx 反向代理联调
- [ ] **T6.4** 服务间通信验证（Frontend → Backend → Analytics）
- [ ] **T6.5** Volume 持久化验证（SQLite 数据不丢失）
- [ ] **T6.6** 一键启动测试 `docker compose up --build`
- [ ] **T6.7** 编写 `README.md` 中的部署说明

---

## Phase 7: 联调与优化

- [ ] **T7.1** 前后端 API 联调（所有页面数据加载正确）
- [ ] **T7.2** Python 预计算结果与前端展示验证
- [ ] **T7.3** 大数据量下的 MySQL 查询性能优化（索引、分页）
- [ ] **T7.4** 前端加载性能优化（数据分页、懒加载）
- [ ] **T7.5** 错误处理与边界情况
- [ ] **T7.6** 响应式布局适配

---

## 优先级建议

| 优先级 | Phase | 说明 |
|---|---|---|
| P0 | Phase 0 | 项目初始化，预计 0.5 天 |
| P0 | Phase 1 | 后端骨架，预计 1 天 |
| P0 | Phase 3 | Python 分析服务（核心算法迁移），预计 2 天 |
| P1 | Phase 4 | 前端骨架，预计 1 天 |
| P1 | Phase 2 | 后端 API 开发，预计 2-3 天 |
| P1 | Phase 5 | 前端页面开发，预计 3-4 天 |
| P2 | Phase 6 | Docker 集成部署，预计 1 天 |
| P2 | Phase 7 | 联调优化，预计 1-2 天 |

**预计总工期**: ~12 天（单人全职开发）

---

## 备注

- 所有数据库密码应通过环境变量注入，不硬编码在代码中
- MySQL 连接使用只读模式，不修改源数据
- Python 服务的预计算结果通过 SQLite 持久化，容器重启后自动恢复
- 后续扩展新仓库只需在 MySQL 中有对应数据，无需代码修改
