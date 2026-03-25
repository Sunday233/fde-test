## Why

项目已完成后端 Spring Boot 3 API 开发（Phase 2）和 Python FastAPI 分析服务（Phase 3），现在需要搭建前端 Vue 3 应用骨架，作为用户界面层对接后端 API，实现费用基线分析、影响因素分析、成本估算等核心功能的可视化展示。前端骨架是 Phase 5（页面开发）的前提。

## What Changes

**所属服务**: frontend (Vue 3)

- 在 `wh-op-platform/frontend/` 目录初始化 Vite + Vue 3 + TypeScript 项目
- 安装并配置 UI 框架：Ant Design Vue 4.x + Tailwind CSS
- 安装并配置图表库：vue-echarts + echarts
- 安装并配置核心依赖：Vue Router、Pinia 状态管理、Axios HTTP 客户端
- 创建主布局 `MainLayout.vue`（左侧导航菜单 + 顶栏 + 内容区）
- 配置路由结构（Dashboard / 费用基线 / 影响因素 / 成本估算 / 报告，共 5 个页面占位）
- 封装 API 请求层 `src/api/index.ts`（Axios 实例、拦截器、后端 API 方法封装）
- 定义 TypeScript 类型（对齐后端 VO 响应结构）
- 编写 Dockerfile（多阶段构建：Node 构建 + Nginx 运行）
- 编写 `nginx.conf`（静态资源服务 + API 反向代理到 backend:8080 和 analytics:8000）

**非目标**:
- 不包含具体页面内容开发（属于 Phase 5）
- 不包含 Docker Compose 集成（属于 Phase 6）
- 不涉及后端或 Python 服务的修改

## Capabilities

### New Capabilities

- `vue-project-skeleton`: Vite + Vue 3 + TypeScript 项目初始化及核心依赖安装配置（Ant Design Vue、Tailwind CSS、vue-echarts、Vue Router、Pinia、Axios）
- `main-layout`: 主布局组件 MainLayout.vue（Ant Design 的 Layout + Sider + Header + Content 结构，左侧菜单含 5 个导航项）
- `frontend-routing`: Vue Router 配置，包含 5 个一级路由及占位页面组件
- `api-layer`: Axios 封装层，包含基础实例配置、请求/响应拦截器、所有后端 API 方法封装
- `frontend-types`: TypeScript 类型定义，对齐后端 Result 包装结构和各 VO 类型
- `frontend-dockerfile`: 多阶段 Docker 构建（Node 构建阶段 + Nginx 运行阶段）及 nginx.conf 反向代理配置

### Modified Capabilities

（无需修改现有 spec）

## Impact

- **新增目录**: `wh-op-platform/frontend/` 下生成完整 Vue 项目结构
- **依赖后端 API**: 所有 6 个后端端点（dashboard/overview、baseline/monthly、impact/factors、estimate/calculate、report/generate、warehouses）+ Python analytics 的 health 端点
- **Nginx 反向代理**: `/api/` 请求转发到 `backend:8080`，`/api/health` 转发到 `analytics:8000`
- **Node.js 依赖**: vue@3、vite@6、ant-design-vue@4、tailwindcss@4、echarts@5、vue-echarts@7、vue-router@4、pinia@2、axios@1
- **后续 Phase 5 依赖此骨架**: 所有页面开发基于此处的路由、布局、API 层和类型定义
