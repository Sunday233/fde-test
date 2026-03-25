## 1. 依赖安装

- [x] 1.1 安装 `markdown-it` 及其 TypeScript 类型声明 (`npm install markdown-it && npm install -D @types/markdown-it`)，用于报告页面 Markdown 渲染

## 2. Dashboard 页面 — KPI 卡片

- [x] 2.1 实现 `DashboardView.vue` 的 KPI 卡片区域：4 张 `a-statistic` 卡片（总单量、总工时、月度费用、人效），调用 `getOverview` API，watch `currentWarehouse` 自动刷新，含 loading 状态

## 3. Dashboard 页面 — 图表

- [x] 3.1 实现日出库单量趋势折线图（ECharts `LineChart`），调用 `getTrend` API，X 轴日期 / Y 轴单量，多仓分 series
- [x] 3.2 实现月度费用构成堆叠柱状图（ECharts `BarChart`），按费用类型堆叠
- [x] 3.3 实现操作类型工作量分布饼图（ECharts `PieChart` 环形），显示百分比 Tooltip

## 4. 费用基线页面 — 筛选与表格

- [x] 4.1 实现 `BaselineView.vue` 的筛选面板：仓库 `a-select` + 月份 `a-date-picker`(picker=month) + 查询按钮，默认加载当前仓库当月数据
- [x] 4.2 实现月度基线数据表格（`a-table`，12 列），调用 `getMonthlyBaseline` API，支持分页，金额列 ¥ 格式化

## 5. 费用基线页面 — 图表

- [x] 5.1 实现劳务单价对比柱状图（ECharts `BarChart`），从 `MonthlyBaselineVO[]` 提取数据
- [x] 5.2 实现双仓月度对比功能：双 `a-select` 选择仓库 + 对比按钮，调用 `compareWarehouses` API，展示分组柱状图

## 6. 影响因素页面 — 因素排序与热力图

- [x] 6.1 实现 `ImpactView.vue` 的因素重要性排序水平柱状图（ECharts `BarChart` 翻转轴），调用 `getFactors` API，正相关蓝色/负相关红色，watch `currentWarehouse`
- [x] 6.2 实现 Pearson 相关系数热力图（ECharts `HeatmapChart`），调用 `getCorrelation` API，使用 `VisualMapComponent` 色阶 -1~1

## 7. 影响因素页面 — 散点图与对比

- [x] 7.1 实现单因素散点图：因素选择器（`a-select`，选项从 `FactorRankVO[]` 获取）+ ECharts `ScatterChart`，调用 `getTrend` API
- [x] 7.2 实现双仓因素对比：双 `a-select` 选仓库，分别调用 `getFactors`，并列展示两仓因素排序图（`a-row` / `a-col` 各 span=12）

## 8. 成本估算页面 — 表单与结果

- [x] 8.1 实现 `EstimateView.vue` 的参数输入表单（`a-form`，8 个 `a-input-number` 字段），调用 `getEstimateDefaults` 加载默认值，含校验规则
- [x] 8.2 实现计算结果展示区域（`a-descriptions` 或 `a-statistic` 卡片组，6 项指标），调用 `calculate` API

## 9. 成本估算页面 — 历史对比

- [x] 9.1 实现与历史数据对比柱状图（ECharts `BarChart`），对比估算值 vs `getWarehouseDetail` 返回的历史值

## 10. 报告页面 — 列表与生成

- [x] 10.1 实现 `ReportView.vue` 的报告列表（`a-table`，含标题/仓库/时间/操作列），调用 `getReportList` API
- [x] 10.2 实现报告生成表单（`a-modal` 弹窗 + `a-form`：仓库选择、开始/结束月份），调用 `generateReport` API，成功后刷新列表

## 11. 报告页面 — 预览与下载

- [x] 11.1 实现 Markdown 在线渲染预览（`a-drawer` 侧拉面板），使用 `markdown-it`（html: false）渲染 `getReportDetail` 返回的内容
- [x] 11.2 实现报告 HTML 下载功能：Markdown → HTML + 内联 CSS 模板 → Blob → 下载

## 12. 构建验证

- [ ] 12.1 运行 `npx vite build` 确保 TypeScript 编译通过、无构建错误
