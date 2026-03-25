## 1. 项目初始化

- [x] 1.1 使用 Vite 创建 Vue 3 + TypeScript 项目骨架（`wh-op-platform/frontend/`），配置 tsconfig 严格模式和路径别名 `@`
- [x] 1.2 安装核心依赖：vue-router@4、pinia@2、axios@1、ant-design-vue@4、@ant-design/icons-vue
- [x] 1.3 安装开发依赖：tailwindcss@4、@tailwindcss/vite、unplugin-vue-components、unplugin-auto-import
- [x] 1.4 安装图表依赖：echarts@5、vue-echarts@7

## 2. 构建工具配置

- [x] 2.1 配置 `vite.config.ts`：注册 Tailwind CSS 插件、unplugin-vue-components（AntDesignVueResolver）、路径别名 `@` → `src/`
- [x] 2.2 创建 `src/assets/main.css`：引入 Tailwind CSS，禁用 preflight 避免与 Ant Design Vue 样式冲突
- [x] 2.3 创建 `src/plugins/echarts.ts`：按需注册 echarts 模块（CanvasRenderer、Bar/Line/Pie/Scatter/Heatmap、Grid/Tooltip/Legend/Title/Toolbox/VisualMap）

## 3. TypeScript 类型定义

- [x] 3.1 创建 `src/types/api.ts`：定义 Result<T> 包装类型和所有 VO 接口（DashboardOverviewVO、TrendDataVO、MonthlyBaselineVO、WarehouseDetailVO、CompareResultVO、FactorRankVO、CorrelationMatrixVO、EstimateRequest、EstimateResultVO、ReportGenerateRequest、ReportVO、WarehouseVO）

## 4. API 请求层

- [ ] 4.1 创建 `src/api/index.ts`：Axios 实例（baseURL: '/api', timeout: 30000）、响应拦截器（Result<T> 解包、错误提示）、所有 API 方法（dashboard、baseline、impact、estimate、report、warehouse 共 12 个方法）

## 5. 状态管理

- [ ] 5.1 创建 `src/stores/app.ts`：定义 useAppStore（currentWarehouse 状态）

## 6. 路由配置

- [ ] 6.1 创建 `src/router/index.ts`：Vue Router 历史模式，5 个懒加载子路由嵌套在 MainLayout 下
- [ ] 6.2 创建 5 个占位视图组件：`src/views/DashboardView.vue`、`BaselineView.vue`、`ImpactView.vue`、`EstimateView.vue`、`ReportView.vue`

## 7. 主布局组件

- [ ] 7.1 创建 `src/layouts/MainLayout.vue`：Ant Design Layout + Sider（可折叠、5 个菜单项带图标）+ Header（仓库选择器）+ Content（RouterView），菜单选中状态与路由同步

## 8. 入口文件

- [ ] 8.1 配置 `src/main.ts`：按序导入 main.css、echarts 插件，创建 App 并注册 Pinia + Router
- [ ] 8.2 配置 `src/App.vue`：仅包含 `<RouterView />`

## 9. Docker 部署配置

- [ ] 9.1 创建 `nginx.conf`：静态文件服务（try_files 兜底 index.html）+ API 反向代理（/api/ → backend:8080）
- [ ] 9.2 创建 `Dockerfile`：两阶段构建（node:20-alpine 构建 → nginx:alpine 运行）
- [ ] 9.3 创建 `.dockerignore`：排除 node_modules、dist、.git 等

## 10. 验证

- [ ] 10.1 执行 `npm install && npm run dev` 验证开发服务器正常启动，访问首页显示 MainLayout + Dashboard 占位页面，侧栏菜单导航正常
