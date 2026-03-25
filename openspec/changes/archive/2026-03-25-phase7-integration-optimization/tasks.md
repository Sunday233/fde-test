## 1. MySQL 索引优化

- [x] 1.1 创建 SQL 迁移脚本 `backend/src/main/resources/db/migration/V1__add_indexes.sql`，包含 5 个复合索引的 `CREATE INDEX IF NOT EXISTS` 语句（出库单表、出勤统计表、报价信息表、工作量统计信息表、入库单表）
- [x] 1.2 在有写权限的 MySQL 连接上执行索引脚本，验证 `SHOW INDEX FROM` 确认索引已创建

## 2. 后端分页支持

- [x] 2.1 新增 `PageResult<T>` 通用分页响应类（records, total, page, size 字段）
- [x] 2.2 为 `GET /api/baseline/monthly` 添加 `page`/`size` 可选参数，Service 层使用 MyBatis-Plus `Page<T>` 分页查询；不传参数时返回全量数据（向后兼容）
- [x] 2.3 为 `GET /api/report/list` 添加 `page`/`size` 可选参数，Service 层使用 MyBatis-Plus `Page<T>` 分页查询
- [x] 2.4 前端 `BaselineView` 表格接入后端分页（`a-table` pagination 配置 + API 参数传递）
- [x] 2.5 前端 `ReportView` 列表接入后端分页

## 3. 前端错误处理增强

- [x] 3.1 Axios 拦截器增强：区分请求取消（`axios.isCancel` → 静默）、超时（`ECONNABORTED` → 专用提示）、5xx（服务器繁忙提示）
- [x] 3.2 为 DashboardView 添加页面级 `error` ref + 错误卡片 + 重试按钮
- [x] 3.3 为 BaselineView 添加页面级 `error` ref + 错误卡片 + 重试按钮
- [x] 3.4 为 ImpactView 添加页面级 `error` ref + 错误卡片 + 重试按钮
- [x] 3.5 为 EstimateView 添加页面级 `error` ref + 错误卡片 + 重试按钮
- [x] 3.6 为 ReportView 添加页面级 `error` ref + 错误卡片 + 重试按钮
- [x] 3.7 为所有空数据场景替换为 `a-empty` 组件（含描述文字）

## 4. 请求取消（AbortController）

- [x] 4.1 在 `api/index.ts` 中创建辅助函数 `useAbortController()`，返回 `{ signal, abort }` 用于 View 集成
- [x] 4.2 为 DashboardView、BaselineView、ImpactView、EstimateView、ReportView 的 `onUnmounted` 添加 AbortController 取消逻辑

## 5. 响应式布局适配

- [x] 5.1 DashboardView KPI 卡片响应式：`span="6"` → `:xs="24" :sm="12" :lg="6"`；双栏图表 `span="12"` → `:xs="24" :md="12"`
- [x] 5.2 BaselineView 双栏布局响应式
- [x] 5.3 ImpactView 双栏布局响应式
- [x] 5.4 EstimateView 表单+结果双栏响应式
- [x] 5.5 MainLayout 侧边栏添加 `breakpoint="md"` 自动折叠 + 仓库选择器宽度自适应（桌面 240px / 移动 160px）
- [x] 5.6 图表容器高度自适应：添加 CSS media query `@media (max-width: 575px)` 将图表高度从 320px 调整为 240px

## 6. 联调验证

- [x] 6.1 本地依次启动 analytics（uvicorn）、backend（mvn spring-boot:run），验证服务健康检查
- [x] 6.2 curl 验证全部 13 个 API 端点返回正确 JSON 结构（`code: 200`），记录失败项并修复
- [x] 6.3 前端 `npm run dev` 通过 Vite 代理连接本地 backend，逐页面验证数据加载和交互
