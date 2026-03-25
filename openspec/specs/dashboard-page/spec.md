## ADDED Requirements

### Requirement: KPI 卡片组件

**所属服务**: frontend  
**文件**: `src/views/DashboardView.vue`

页面顶部 SHALL 展示 4 张 KPI 统计卡片，使用 Ant Design Vue `a-statistic` 组件，横向排列于 `a-row` / `a-col` 中（每行 4 列，`span=6`）。

卡片内容：
1. **总单量** — `DashboardOverviewVO.totalOrders`，后缀"单"
2. **总工时** — `DashboardOverviewVO.totalWorkHours`，后缀"h"，保留 1 位小数
3. **月度费用** — `DashboardOverviewVO.monthlyFee`，前缀"¥"，保留 2 位小数
4. **人效** — `DashboardOverviewVO.laborEfficiency`，后缀"件/人时"，保留 2 位小数

**数据流**: `onMounted` + `watch(currentWarehouse)` → 调用 `getOverview(warehouseCode, month)` → 填充卡片数据。月份默认取当前月。

#### Scenario: 正常展示 KPI 卡片
- **WHEN** 用户进入 Dashboard 页面且已选择仓库
- **THEN** 页面 SHALL 调用 `getOverview` API 并展示 4 张 KPI 卡片，数据与 API 返回一致

#### Scenario: 切换仓库刷新数据
- **WHEN** 用户在顶栏切换仓库选择
- **THEN** KPI 卡片 SHALL 自动重新加载对应仓库的数据

#### Scenario: 加载中状态
- **WHEN** API 正在请求中
- **THEN** 卡片区域 SHALL 展示 `a-spin` 加载动画

### Requirement: 日出库单量趋势折线图

**所属服务**: frontend  
**文件**: `src/views/DashboardView.vue`

页面 SHALL 展示一个折线图（ECharts `LineChart`），展示按日的出库单量趋势。

**数据源**: 调用 `getTrend(warehouseCode, startMonth, endMonth, 'outbound_orders')` → `TrendDataVO[]`

**图表配置**:
- X 轴: 日期（`TrendDataVO.date`）
- Y 轴: 出库单量（`TrendDataVO.value`）
- 多仓时按 `warehouseName` 分 series 展示
- 启用 `TooltipComponent`（十字准星 trigger: 'axis'）
- 启用 `LegendComponent`

#### Scenario: 展示日趋势折线图
- **WHEN** Dashboard 页面加载完成
- **THEN** SHALL 展示日出库单量折线图，X 轴为日期，Y 轴为单量

#### Scenario: 数据为空
- **WHEN** API 返回空数组
- **THEN** 图表区域 SHALL 展示空状态提示

### Requirement: 月度费用构成柱状图

**所属服务**: frontend  
**文件**: `src/views/DashboardView.vue`

页面 SHALL 展示一个堆叠柱状图（ECharts `BarChart`），展示月度费用构成。

**数据源**: 调用 `getTrend(warehouseCode, startMonth, endMonth, 'monthly_fee_breakdown')` → `TrendDataVO[]`，其中 `type` 字段区分费用类型（固定劳务、临时劳务、叉车劳务等）。

**图表配置**:
- X 轴: 月份
- Y 轴: 费用金额（元）
- 按 `type` 分 series，stack 堆叠
- 启用 `TooltipComponent` + `LegendComponent`

#### Scenario: 展示费用构成柱状图
- **WHEN** Dashboard 页面加载完成
- **THEN** SHALL 展示月度费用构成柱状图，按费用类型堆叠

### Requirement: 操作类型工作量分布图

**所属服务**: frontend  
**文件**: `src/views/DashboardView.vue`

页面 SHALL 展示一个饼图（ECharts `PieChart`），展示各操作类型的工作量占比。

**数据源**: 调用 `getTrend(warehouseCode, startMonth, endMonth, 'workload_distribution')` → `TrendDataVO[]`，其中 `type` 字段为操作类型名称，`value` 为工作量。

**图表配置**:
- 数据: `{ name: type, value: value }`
- 启用 `TooltipComponent`（显示百分比）+ `LegendComponent`
- 环形样式: `radius: ['40%', '70%']`

#### Scenario: 展示操作类型分布饼图
- **WHEN** Dashboard 页面加载完成
- **THEN** SHALL 展示操作类型工作量分布饼图，显示各类型占比

#### Scenario: Tooltip 交互
- **WHEN** 用户悬停某饼图扇区
- **THEN** SHALL 显示该操作类型的名称、数值和百分比
