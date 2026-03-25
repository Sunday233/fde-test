## Context

Phase 4 已完成 Vue 3 前端骨架搭建：Vite 构建、Vue Router 5 路由、Pinia 状态管理、Ant Design Vue 4.x UI 框架（auto-import）、Tailwind CSS 4（仅 utilities 层）、ECharts（vue-echarts 懒注册）、Axios API 封装层（12 个方法）、TypeScript 类型定义（13 个接口）。

当前 5 个视图文件（DashboardView、BaselineView、ImpactView、EstimateView、ReportView）均为 `<h1>` + `页面开发中...` 的占位符。本阶段需将它们替换为完整的交互式页面。

**数据流**：页面 → `src/api/index.ts`（Axios） → Nginx `/api/` 反向代理 → Spring Boot 后端 → MySQL（只读）/ Python FastAPI（预计算结果在 SQLite）

**仓库选择**：`MainLayout.vue` 顶栏的仓库下拉框绑定 `useAppStore().currentWarehouse`，各页面 watch 此值触发数据重新加载。

## Goals / Non-Goals

**Goals:**
- 实现 5 个功能页面的完整 UI 和数据交互逻辑
- 所有图表使用 vue-echarts + 已注册的 ECharts 模块（Bar/Line/Pie/Scatter/Heatmap）
- 表格使用 Ant Design Vue 的 `a-table`，表单使用 `a-form`
- 页面根据 `currentWarehouse` 变化自动刷新数据
- 报告页面支持 Markdown 渲染预览和 HTML 下载

**Non-Goals:**
- 不新增后端 API 或 Python 分析接口
- 不做移动端响应式适配
- 不做权限控制或登录功能
- 不做前端单元测试（Phase 7 范畴）
- 不抽取通用图表组件库（按页面内聚实现，避免过度抽象）

## Decisions

### D1: 页面内聚 vs 组件抽取

**决定**：每个页面视图文件自包含其所有图表和交互逻辑，不抽取独立的 `components/` 目录。

**理由**：5 个页面间图表配置差异大（不同的 series/tooltip/legend），强行抽取会增加 props 传递复杂度。当前仅 2 个仓库的数据规模，页面内聚更直观。如果未来需要复用，可在 Phase 7 重构。

**替代方案**：抽取 `ChartCard.vue` 通用包装组件。放弃原因：每个图表的 option 构建逻辑差异大，包装组件只能处理 loading/empty 状态，投入产出比低。

### D2: 数据加载模式

**决定**：每个页面 `onMounted` + `watch(currentWarehouse)` 触发数据加载，使用 `ref` 管理 loading/data/error 状态。不使用额外的 Pinia store 缓存页面数据。

**理由**：各页面数据独立且依赖筛选条件。将数据缓存到 Pinia 会增加复杂度且收益低（数据量小，API 响应快）。

**替代方案**：使用 VueQuery/TanStack Query 做缓存。放弃原因：引入额外依赖，MVP 不需要。

### D3: ECharts 图表类型映射

各页面使用的 ECharts 图表类型：

| 页面 | 图表 | ECharts 类型 | 组件 |
|---|---|---|---|
| Dashboard | 日出库趋势 | `LineChart` | GridComponent, TooltipComponent |
| Dashboard | 月度费用构成 | `BarChart` | GridComponent, TooltipComponent, LegendComponent |
| Dashboard | 操作类型分布 | `PieChart` | TooltipComponent, LegendComponent |
| Baseline | 劳务单价对比 | `BarChart` | GridComponent, TooltipComponent |
| Baseline | 双仓月度对比 | `BarChart` | GridComponent, TooltipComponent, LegendComponent |
| Impact | 因素排序 | `BarChart` (水平) | GridComponent, TooltipComponent |
| Impact | 相关系数热力图 | `HeatmapChart` | GridComponent, TooltipComponent, VisualMapComponent |
| Impact | 散点图 | `ScatterChart` | GridComponent, TooltipComponent |
| Estimate | 历史对比 | `BarChart` | GridComponent, TooltipComponent, LegendComponent |

所有图表类型和组件已在 `plugins/echarts.ts` 中注册，无需新增。

### D4: Markdown 渲染方案

**决定**：使用 `markdown-it` 库在前端渲染报告 Markdown 内容。

**理由**：轻量（~30KB gzip）、纯前端渲染、支持插件扩展。`getReportDetail` API 返回的 `ReportVO` 包含 Markdown 内容，前端直接渲染为 HTML。

**替代方案**：`marked`——功能类似但 `markdown-it` 插件生态更丰富，XSS 防护更好（默认不渲染 HTML 标签）。

### D5: HTML 报告下载

**决定**：前端将渲染后的 HTML 包裹在完整的 HTML 文档模板中（含内联 CSS），通过 `Blob` + `URL.createObjectURL` 触发浏览器下载。

**理由**：纯前端实现，无需后端增加导出接口。用户可直接用浏览器打开下载的 HTML 文件。

### D6: 费用计算公式（Estimate 页面展示）

成本估算页面的公式（与后端 `POST /api/estimate/calculate` 一致）：

```
预估月度总件数 = 日均单量 × 件单比 × 工作天数
预估月度总工时(h) = 预估月度总件数 / 人效(件/人时)
预估人数 = 预估月度总工时 / (工作天数 × 8)
加权平均单价 = 固定劳务单价 × 固临比 + 临时劳务单价 × (1 - 固临比)
月度操作费用 = 预估月度总工时 × 加权平均单价 × (1 + 税率)
单均成本 = 月度操作费用 / (日均单量 × 工作天数)
件均成本 = 月度操作费用 / 预估月度总件数
```

前端仅提交参数到后端计算，不在前端重复实现公式。

### D7: 月份选择器格式

**决定**：使用 Ant Design Vue 的 `a-date-picker` 组件，设置 `picker="month"` 和 `format="YYYY-MM"`，值格式统一为 `"YYYY-MM"` 字符串传给后端 API。

### D8: 双仓对比交互模式

**决定**：Baseline 和 Impact 页面的双仓对比使用两个 `a-select` 分别选择仓库 A 和仓库 B，提交后调用 `compareWarehouses` / `getFactors` 对比接口。

**理由**：当前仅 2 个仓库，简单的双选择器即可满足需求。未来仓库增多时可升级为穿梭框或多选。

## Risks / Trade-offs

- **[API 返回格式不匹配]** → 前端 TypeScript 接口基于 Phase 2 设计推导，实际后端可能有字段差异。缓解：Phase 7 联调时修正。
- **[大数据量图表性能]** → 日趋势图如果时间跨度长（1年 × 多仓），数据点多。缓解：后端分页或聚合，前端使用 ECharts `dataZoom` 组件。
- **[Markdown XSS 风险]** → `markdown-it` 默认不渲染 HTML 标签（`html: false`），可防止 XSS。需确保不开启 `html` 选项。
- **[离线状态下的空数据展示]** → API 不可用时页面应显示友好的空状态。缓解：各图表/表格组件统一处理 loading 和 empty 状态。
