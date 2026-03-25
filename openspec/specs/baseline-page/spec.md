## ADDED Requirements

### Requirement: 筛选面板

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

页面顶部 SHALL 展示一个筛选面板（`a-card` 包裹），包含以下筛选项横向排列：

1. **仓库选择** — `a-select`，选项来自 `getWarehouses()` API，默认选中 `currentWarehouse`
2. **年月选择** — `a-date-picker` 设置 `picker="month"` + `format="YYYY-MM"`，默认当前月
3. **查询按钮** — `a-button type="primary"`，点击触发数据加载

**数据流**: 点击查询 → 调用 `getMonthlyBaseline(warehouseCode, year, month)` + `getWarehouseDetail(warehouseCode, year, month)` → 更新表格和图表。

#### Scenario: 筛选面板初始化
- **WHEN** 用户进入费用基线页面
- **THEN** 筛选面板 SHALL 展示仓库选择器（默认当前仓库）和月份选择器（默认当前月），并自动加载数据

#### Scenario: 点击查询
- **WHEN** 用户修改筛选条件并点击查询按钮
- **THEN** 页面 SHALL 根据新条件重新加载表格和图表数据

### Requirement: 月度基线数据表格

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

筛选面板下方 SHALL 展示一个数据表格（`a-table`），展示月度基线汇总数据。

**数据源**: `getMonthlyBaseline()` → `MonthlyBaselineVO[]`

**表格列定义**:
| 列标题 | 字段 | 格式 |
|---|---|---|
| 仓库 | warehouseName | 文本 |
| 年份 | year | 数字 |
| 月份 | month | 数字 |
| 日均费用 | dailyAvgFee | ¥ 保留2位 |
| 总费用 | totalFee | ¥ 保留2位 |
| 总单量 | totalOrders | 千分位 |
| 总件数 | totalItems | 千分位 |
| 单均成本 | costPerOrder | ¥ 保留2位 |
| 件均成本 | costPerItem | ¥ 保留2位 |
| 平均人数 | avgHeadcount | 保留1位 |
| 总工时 | totalWorkHours | 保留1位 h |
| 加权单价 | weightedUnitPrice | ¥ 保留2位 /h |

**功能**: 支持分页（`pagination`）、行 key 用 `warehouseCode + year + month`。

#### Scenario: 展示月度基线表格
- **WHEN** 基线数据加载完成
- **THEN** SHALL 展示含 12 列的表格，数据与 API 返回匹配

#### Scenario: 空数据
- **WHEN** 筛选条件下无数据
- **THEN** 表格 SHALL 展示 Ant Design Vue 默认空状态

### Requirement: 劳务单价对比图

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

表格下方 SHALL 展示一个柱状图（ECharts `BarChart`），对比各仓库的加权平均劳务单价。

**数据源**: 从 `MonthlyBaselineVO[]` 提取 `warehouseName` 和 `weightedUnitPrice`。

**图表配置**:
- X 轴: 仓库名称
- Y 轴: 单价（元/h）
- 每个仓库一个柱子，不同月份用不同颜色区分
- 启用 `TooltipComponent`

#### Scenario: 展示劳务单价对比
- **WHEN** 基线数据加载完成
- **THEN** SHALL 展示各仓库的加权平均单价柱状图

### Requirement: 双仓月度对比图

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

页面 SHALL 提供双仓对比功能：两个 `a-select` 选择仓库 A 和仓库 B，点击"对比"按钮后展示分组柱状图。

**数据源**: 调用 `compareWarehouses([codeA, codeB], year, month)` → `CompareResultVO[]`

**图表配置** (ECharts `BarChart`):
- X 轴: 对比维度（总费用、总单量、单均成本、件均成本、平均人数）
- Y 轴: 数值
- 两个 series，分别对应仓库 A 和仓库 B
- 启用 `TooltipComponent` + `LegendComponent`

#### Scenario: 双仓对比
- **WHEN** 用户选择两个仓库并点击对比按钮
- **THEN** SHALL 展示分组柱状图，并列对比两仓的核心指标

#### Scenario: 未选齐仓库
- **WHEN** 用户未选择两个仓库就点击对比
- **THEN** SHALL 提示用户选择两个仓库
