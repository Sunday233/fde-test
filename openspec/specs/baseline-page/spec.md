## ADDED Requirements

### Requirement: 区间筛选面板

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

页面顶部 SHALL 展示一个筛选面板，包含以下查询条件：

1. **开始月份**: `a-date-picker`，`picker="month"`，格式 `YYYY-MM`
2. **结束月份**: `a-date-picker`，`picker="month"`，格式 `YYYY-MM`
3. **仓库筛选**: `a-select`，选项来自 `getWarehouses()`，允许为空，为空时表示全部仓库
4. **查询按钮**: `a-button type="primary"`

页面 SHALL 不再依赖全局顶部仓库切换器作为费用基线页面的数据源。

#### Scenario: 默认加载最近一年数据
- **WHEN** 用户进入费用基线页面
- **THEN** 页面 SHALL 调用 `getLatestBaselineMonth()` 获取最新可用月份
- **AND** 默认设置 `endMonth = latestMonth`
- **AND** 默认设置 `startMonth = latestMonth - 11 months`
- **AND** 默认加载所有仓库在最近 12 个月内的月度基线数据

#### Scenario: 手动查询区间数据
- **WHEN** 用户修改月份区间或仓库后点击查询按钮
- **THEN** 页面 SHALL 调用月度基线接口与每日明细接口重新加载页面数据

### Requirement: 月度基线数据表格

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

筛选面板下方 SHALL 展示月度基线数据表格，支持分页。

**数据源**: `getMonthlyBaseline(warehouseCode, startMonth, endMonth, page, size)`

**表格列定义**:
| 列标题 | 字段 | 格式 |
|---|---|---|
| 仓库 | warehouseName | 文本 |
| 年份 | year | 数字 |
| 月份 | month | 数字 |
| 日均费用 | dailyAvgFee | ¥ 保留 2 位 |
| 总费用 | totalFee | ¥ 保留 2 位 |
| 总单量 | totalOrders | 千分位 |
| 总件数 | totalItems | 千分位 |
| 件单比 | itemsPerOrder | 保留 2 位 |
| 日均单量 | dailyAvgOrders | 保留 0 位 |
| 单均成本 | costPerOrder | ¥ 保留 2 位 |
| 件均成本 | costPerItem | ¥ 保留 2 位 |
| 平均人数 | avgHeadcount | 保留 1 位 |
| 人效 | laborEfficiency | 保留 2 位 + `件/人时` |
| 固临比 | fixedTempRatio | 保留 2 位 |
| 总工时 | totalWorkHours | 保留 1 位 + `h` |
| 加权单价 | weightedUnitPrice | ¥ 保留 2 位 `/h` |

数据 SHALL 按月份倒序展示，同月内可包含多个仓库。

#### Scenario: 展示区间内多月数据
- **WHEN** 用户查询一个跨月区间
- **THEN** 表格 SHALL 展示该区间内所有月份的记录，而不是仅展示单月结果

### Requirement: 每日操作费用明细表

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

月度基线表格下方 SHALL 展示每日操作费用分析明细表，用于替代原“加权平均劳务单价对比柱状图”。

**数据源**: `getDailyDetail(warehouseCode, startMonth, endMonth)`

**表格列定义**:
| 列标题 | 字段 | 格式 |
|---|---|---|
| 日期 | date | `YYYY-MM-DD` |
| 仓库 | warehouseName | 文本 |
| 出库单量 | obOrders | 千分位 |
| 出库件数 | obItems | 千分位 |
| 件单比 | itemOrderRatio | 保留 2 位 |
| 出勤人数 | headcount | 整数 |
| 工时(h) | workHours | 保留 2 位 |
| 当日费用(¥) | dailyFee | ¥ 保留 2 位 |

该表 SHALL 支持分页，默认每页 30 条。

#### Scenario: 无每日明细数据
- **WHEN** 当前筛选条件下不存在每日费用明细
- **THEN** 页面 SHALL 展示空状态提示

### Requirement: 双仓月度趋势对比

**所属服务**: frontend  
**文件**: `src/views/BaselineView.vue`

页面 SHALL 提供双仓趋势对比区域，包含：

1. 仓库 A 选择器
2. 仓库 B 选择器
3. 对比按钮
4. 指标切换按钮组
5. 分组柱状图

**数据源**: `compareWarehouses([codeA, codeB], startMonth, endMonth)`

图表 SHALL 使用月份作为 X 轴，并基于以下指标进行切换：
- 总费用 `totalFee`
- 总单量 `totalOrders`
- 单均成本 `costPerOrder`
- 件均成本 `costPerItem`
- 平均人数 `avgHeadcount`

#### Scenario: 双仓区间趋势对比
- **WHEN** 用户选择两个不同仓库并点击对比按钮
- **THEN** 页面 SHALL 请求区间内两仓的月度对比数据
- **AND** 图表 SHALL 以月份为 X 轴展示两个仓库的趋势对比

#### Scenario: 仓库未选择完整
- **WHEN** 用户未选择两个不同仓库
- **THEN** 对比按钮 SHALL 不可用，页面不发起对比请求