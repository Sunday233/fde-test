## ADDED Requirements

### Requirement: 影响因素重要性排序柱状图

**所属服务**: frontend  
**文件**: `src/views/ImpactView.vue`

页面 SHALL 展示一个水平柱状图（ECharts `BarChart`，Y 轴为类目轴），按相关系数绝对值从高到低排列影响因素。

**数据源**: 调用 `getFactors(warehouseCode)` → `FactorRankVO[]`

**图表配置**:
- Y 轴: 因素名称（`factorName`），按 `rank` 排序
- X 轴: 相关系数绝对值（`|correlation|`）
- 柱子颜色: 正相关用蓝色，负相关用红色
- 启用 `TooltipComponent`，显示因素描述（`description`）和精确相关系数

**数据流**: `onMounted` + `watch(currentWarehouse)` → 调用 API 加载数据。

#### Scenario: 展示因素排序图
- **WHEN** 用户进入影响因素页面且已选择仓库
- **THEN** SHALL 展示按相关系数绝对值降序排列的水平柱状图

#### Scenario: 区分正负相关
- **WHEN** 因素与费用为正相关
- **THEN** 柱子 SHALL 显示为蓝色
- **WHEN** 因素与费用为负相关
- **THEN** 柱子 SHALL 显示为红色

#### Scenario: 仓库切换刷新
- **WHEN** 用户在顶栏切换仓库
- **THEN** 因素排序图 SHALL 自动重新加载对应仓库数据

### Requirement: Pearson 相关系数热力图

**所属服务**: frontend  
**文件**: `src/views/ImpactView.vue`

页面 SHALL 展示一个热力图（ECharts `HeatmapChart`），展示各因素间的 Pearson 相关系数矩阵。

**数据源**: 调用 `getCorrelation(warehouseCode)` → `CorrelationMatrixVO`（包含 `factors: string[]` 和 `matrix: number[][]`）

**图表配置**:
- X 轴 / Y 轴: 因素名称列表（`factors`）
- 数据: 将 `matrix[i][j]` 转换为 `[i, j, value]` 格式
- 使用 `VisualMapComponent`，范围 -1 到 1，蓝（负）→ 白（0）→ 红（正）
- 启用 `TooltipComponent`，显示 `factors[i]` × `factors[j]` = `matrix[i][j]`

#### Scenario: 展示相关系数热力图
- **WHEN** 影响因素页面加载完成
- **THEN** SHALL 展示 N×N 的 Pearson 相关系数热力图

#### Scenario: Tooltip 交互
- **WHEN** 用户悬停热力图某单元格
- **THEN** SHALL 显示两个因素名称及其相关系数值

### Requirement: 单因素散点图

**所属服务**: frontend  
**文件**: `src/views/ImpactView.vue`

页面 SHALL 提供一个因素选择器（`a-select`），选择后展示该因素与工时的散点图（ECharts `ScatterChart`）。

**数据源**: 调用 `getTrend(warehouseCode, startMonth, endMonth, factorName)` → `TrendDataVO[]`。X 为选定因素值，Y 为工时值。

**图表配置**:
- X 轴: 选定因素值
- Y 轴: 工时
- 每个点代表一个日维度样本
- 启用 `TooltipComponent`，显示日期和两轴数值

**因素选择器**: 选项从 `FactorRankVO[].factorName` 获取，默认选第一个因素。

#### Scenario: 展示散点图
- **WHEN** 用户选择一个影响因素
- **THEN** SHALL 展示该因素与工时的散点图

#### Scenario: 切换因素
- **WHEN** 用户从下拉框选择不同因素
- **THEN** 散点图 SHALL 自动更新为新因素的数据

### Requirement: 双仓因素对比

**所属服务**: frontend  
**文件**: `src/views/ImpactView.vue`

页面 SHALL 提供双仓因素对比功能：两个 `a-select` 选择仓库 A 和仓库 B，并列展示两仓的因素排序图。

**数据流**: 分别调用 `getFactors(codeA)` 和 `getFactors(codeB)`，将结果并列展示为两个水平柱状图（或同一图中双 series）。

**布局**: 使用 `a-row` + `a-col`（各 `span=12`），左侧仓库 A，右侧仓库 B。

#### Scenario: 双仓对比展示
- **WHEN** 用户选择两个仓库并点击对比
- **THEN** SHALL 并列展示两仓的因素重要性排序图

#### Scenario: 单仓无数据
- **WHEN** 某仓库无因素分析数据
- **THEN** 对应侧 SHALL 展示空状态提示
