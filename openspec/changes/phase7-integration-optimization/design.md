## Context

项目三层服务已独立完成：前端 5 个页面（Dashboard/Baseline/Impact/Estimate/Report）、后端 11 个 REST API + AnalyticsClient 桥接、Python 分析服务 3 阶段预计算流水线。Docker Compose 编排已就绪。当前存在以下技术债务：

- MySQL 查询无索引，700 万行+数据量下聚合查询可能全表扫描
- 前端数据表格一次性加载全量数据，无后端分页
- 错误处理仅有全局 toast，无页面级重试或降级 UI
- 布局仅桌面端，Ant Design Grid `span` 固定值无响应式断点

服务间调用链路：
- 浏览器 → Nginx(:80) → Backend(:8080) → MySQL(只读) + Analytics(:8000)
- Analytics → MySQL(只读) + SQLite(读写)

## Goals / Non-Goals

**Goals:**
- 前后端 API 联调通过，所有页面数据正确加载
- MySQL 高频查询有索引覆盖，聚合查询响应时间 < 2s
- 前端表格支持后端分页（每页 20 条），减少首次加载数据量
- 每个页面有错误状态 UI（重试按钮）和增强的空数据提示
- 关键布局在 ≤ 768px 宽度下正常显示（单列堆叠）

**Non-Goals:**
- 不引入 Redis/Memcached 缓存层
- 不做 WebSocket 实时推送
- 不做移动端原生适配
- 不修改业务逻辑或计算公式
- 不新增 API 端点

## Decisions

### D1: MySQL 索引策略 — 复合索引优先

为高频聚合查询创建复合索引：
- `出库单表`: `idx_outbound_wh_time (库房编码, 创建时间)`
- `出勤统计表`: `idx_attendance_wh_date (库房, 考勤日期)`
- `报价信息表`: `idx_quote_wh_status (库房名称, 报价状态)`
- `工作量统计信息表`: `idx_workload_wh_month (库房编码, 月份)`
- `入库单表`: `idx_inbound_wh_time (库房编码, 创建时间)`

**理由**：复合索引覆盖 WHERE + GROUP BY 模式，避免全表扫描。MySQL 5.7 的 B-Tree 索引对范围查询友好。只读库不影响写入性能。

**替代方案**：覆盖索引（包含 SELECT 列）— 空间开销大，当前数据量无必要。

### D2: 后端分页 — MyBatis-Plus Page 插件

对 `getMonthlyBaseline` 和 `getReportList` 添加分页参数（`page`, `size`），使用 MyBatis-Plus `Page<T>` 分页对象。返回结构扩展为 `PageResult<T>`，包含 `records[]`、`total`、`page`、`size`。

**理由**：MyBatis-Plus 内置分页插件，零侵入，仅需在 Service 层包裹 `Page` 对象。前端 `a-table` 原生支持分页参数。

**替代方案**：前端虚拟滚动 — 仍需加载全量数据到浏览器，不解决网络传输和后端查询压力。

### D3: 前端错误处理 — 页面级 error ref + 重试

每个 View 新增 `error` ref（`Ref<string | null>`），在 `catch` 中赋值错误信息。模板中根据 `error` 显示错误卡片（含重试按钮 + 错误描述）。保留现有全局 toast 作为补充。

**理由**：用户看到错误后能直接点重试，而非刷新整个页面。实现简单，每个 View 约 10 行改动。

**替代方案**：全局 ErrorBoundary 组件 — Vue 3 无原生 Error Boundary，需手动 `onErrorCaptured`，过度设计。

### D4: 响应式策略 — Ant Design Grid 断点属性

使用 `a-col` 的响应式属性（`:xs="24" :sm="12" :lg="6"`）替代固定 `:span`。关键断点：
- `xs` (< 576px): 单列
- `sm` (≥ 576px): 双列
- `lg` (≥ 992px): 四列（Dashboard KPI 卡片）

侧边栏在 `<= 768px` 默认折叠（`collapsed` 初始值根据窗口宽度判断）。

**理由**：Ant Design Vue 的 Grid 内置断点系统，零 CSS 编写。`a-layout-sider` 的 `breakpoint` 属性可自动折叠。

### D5: 联调验证 — 本地启动 + curl 验证

在本地分别启动 backend 和 analytics 服务，通过 curl/浏览器逐一验证 13 个 API 端点的响应格式和数据正确性。前端通过 `vite dev --proxy` 连接本地 backend。

**理由**：Docker 未安装，使用本地服务直连验证。Vite dev server 已配置 `/api` 代理。

### D6: 请求取消 — AbortController

为每个 View 的 `onUnmounted` 添加请求取消逻辑，使用 `AbortController` + Axios `signal` 参数。避免用户快速切换页面时旧请求响应覆盖新数据。

**理由**：轻量级改动（每个 View 约 5 行），防止竞态条件。Vue 3 `onUnmounted` 生命周期钩子天然适合。

## Risks / Trade-offs

- **[MySQL 索引创建权限]** 只读连接可能无 CREATE INDEX 权限 → 缓解：提供 SQL 脚本，由 DBA 或有权限的账号执行
- **[分页破坏现有图表]** 图表依赖全量数据绘制，分页后数据不完整 → 缓解：仅对纯表格（Baseline 列表、Report 列表）做分页，图表数据仍全量加载
- **[响应式改动范围]** 5 个 View + Layout 都需改动 → 缓解：统一模式（xs/sm/lg 断点），批量替换
- **[联调环境差异]** 本地 MySQL 连接与 Docker 内连接路径不同 → 缓解：.env 已区分，联调仅验证 API 契约正确性
