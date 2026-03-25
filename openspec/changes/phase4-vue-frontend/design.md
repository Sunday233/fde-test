## Context

后端 API 层（Spring Boot 3）和 Python 分析服务（FastAPI）已完成开发部署。后端提供 6 个 REST API 端点（dashboard/overview、baseline/monthly、impact/factors、estimate/calculate、report/generate、warehouses），Python 服务提供 impact/correlation 和 baseline/daily-metrics 端点。

前端目录 `wh-op-platform/frontend/` 当前为空（仅含 `.gitkeep`）。本次设计覆盖 Vue 3 项目的完整搭建，为 Phase 5 页面开发提供可运行的骨架。

## Goals / Non-Goals

**Goals:**
- 搭建可运行的 Vue 3 + Vite + TypeScript 前端项目
- 配置 UI 框架（Ant Design Vue 4.x）和实用 CSS（Tailwind CSS）
- 配置图表库（vue-echarts + echarts）供后续页面使用
- 建立主布局结构（侧栏导航 + 顶栏 + 内容区）
- 配置 5 个一级路由及占位页面
- 封装 API 请求层，对齐后端所有端点
- 定义 TypeScript 类型，匹配后端 VO 结构
- 提供 Dockerfile + nginx.conf 实现容器化部署

**Non-Goals:**
- 不开发具体页面内容和交互逻辑（Phase 5）
- 不配置 Docker Compose 编排（Phase 6）
- 不实现用户认证/权限控制（第一版无认证）
- 不对后端或 Python 服务做任何修改

## Decisions

### D1: Vite 6 + Vue 3.5 + TypeScript

**选择**: Vite 6 作为构建工具，Vue 3.5 + `<script setup>` SFC，TypeScript 严格模式。

**替代方案**: Vue CLI (webpack) — 已被 Vue 官方弃用，Vite 是推荐工具链。

**理由**: Vite 开发启动快、HMR 即时，Vue 3 Composition API + TypeScript 提供良好的类型推导和代码组织能力。

### D2: Ant Design Vue 4.x 按需加载 + Tailwind CSS 4

**选择**: Ant Design Vue 4.x 通过 `unplugin-vue-components` 实现按需自动导入；Tailwind CSS 4 用于布局微调和间距控制。

**替代方案**: Element Plus — 功能类似，但 Ant Design Vue 与项目整体需求的 ProLayout 风格更匹配。

**理由**: Ant Design Vue 提供完善的 Layout / Menu / Table / Form 组件，覆盖本项目所有 UI 需求。Tailwind 补充原子化样式，减少自定义 CSS。两者通过 CSS 层级共存，不冲突。

### D3: vue-echarts 7 + echarts 5 按需引入

**选择**: `vue-echarts` 作为 Vue 封装层，`echarts` 按需引入需要的图表类型（折线图、柱状图、饼图、散点图、热力图）。

**替代方案**: 直接使用 echarts — 需要手动管理 DOM 操作和生命周期。

**理由**: vue-echarts 提供响应式的 Vue 组件接口（`<v-chart :option="...">`），自动处理 resize 和销毁。按需引入减小包体积。

### D4: Vue Router 历史模式 + 扁平路由结构

**选择**: Vue Router 4，`createWebHistory` 历史模式，5 个一级路由平铺在 `MainLayout` 下。

路由表：
| 路径 | 名称 | 组件 |
|---|---|---|
| `/` | Dashboard | `views/DashboardView.vue` |
| `/baseline` | 费用基线 | `views/BaselineView.vue` |
| `/impact` | 影响因素 | `views/ImpactView.vue` |
| `/estimate` | 成本估算 | `views/EstimateView.vue` |
| `/report` | 报告 | `views/ReportView.vue` |

**理由**: 当前 5 个页面属于一级导航，无需嵌套路由。历史模式 URL 更干净，Nginx 配置 `try_files` 支持。

### D5: Pinia 状态管理

**选择**: Pinia 2.x 作为全局状态管理，初始创建 `useAppStore`（当前仓库筛选等全局状态）。

**理由**: Pinia 是 Vue 3 官方推荐的状态管理库，TypeScript 支持完善，API 简洁。初期仅创建 store 骨架，具体 store 在 Phase 5 按需扩展。

### D6: Axios 封装 + 统一拦截器

**选择**: Axios 1.x，创建带 `baseURL: '/api'` 的实例，响应拦截器统一处理后端 `Result<T>` 包装结构（提取 `data` 字段，`code !== 200` 时显示 `message` 错误提示）。

数据流：
```
Vue 组件 → api/index.ts → Axios 实例 → Nginx 反向代理 → backend:8080 / analytics:8000
```

**理由**: 统一拦截器避免每个 API 调用重复处理错误和数据解包。`baseURL: '/api'` 配合 Nginx 反向代理实现服务透明迁移。

### D7: Nginx 多级反向代理

**选择**: Nginx 作为前端静态资源服务器，同时代理 API 请求：
- `/api/health` → `analytics:8000`（Python 健康检查直通）
- `/api/impact/factors`、`/api/impact/correlation`、`/api/baseline/daily-metrics` → `analytics:8000`（前端直调 Python 服务的特定端点）
- `/api/` → `backend:8080`（其余 API 走 Spring Boot）
- `/` → 静态文件 `try_files`

**替代方案**: 所有 API 统一走 backend，由 backend 代理 analytics — 已有 AnalyticsClient.java 实现此模式。

**理由**: 保持与现有后端 AnalyticsClient 一致，所有 `/api/` 请求统一转发到 `backend:8080`，由后端内部调用 Python 服务。简化 Nginx 配置，前端不需要知道 analytics 服务地址。

修正后的 Nginx 代理规则：
- `/api/` → `backend:8080`（后端统一入口）
- `/` → 静态文件 `try_files`

### D8: 多阶段 Docker 构建

**选择**: 两阶段构建：
1. `node:20-alpine` — 安装依赖、构建产物
2. `nginx:alpine` — 复制 dist/ 和 nginx.conf，暴露端口 80

**理由**: 最终镜像仅包含 Nginx + 静态文件，体积约 30MB。

## Risks / Trade-offs

- **[Tailwind 与 Ant Design 样式冲突]** → Tailwind 的 preflight 可能重置 Ant Design 基础样式。缓解：Tailwind 4 配置中禁用 preflight 或设置 `important` 前缀。
- **[ECharts 按需引入遗漏]** → Phase 5 新增图表类型时需手动注册对应 echarts 模块。缓解：在 `plugins/echarts.ts` 集中管理注册，便于维护。
- **[TypeScript 类型与后端 VO 不同步]** → 后端 VO 变更时前端类型需手动更新。缓解：类型定义集中在 `types/api.ts`，Phase 7 联调时统一校验。
- **[Nginx 代理路径变更]** → Docker Compose 服务名变更时需同步更新 nginx.conf。缓解：使用环境变量或 Docker 内部 DNS。
