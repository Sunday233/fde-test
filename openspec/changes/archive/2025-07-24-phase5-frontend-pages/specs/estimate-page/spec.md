## ADDED Requirements

### Requirement: 参数输入表单

**所属服务**: frontend  
**文件**: `src/views/EstimateView.vue`

页面 SHALL 展示一个参数输入表单（`a-form`），用于填写成本估算所需参数。

**表单字段**（对应 `EstimateRequest` 接口）:
| 字段 | 标签 | 组件 | 校验 | 默认值来源 |
|---|---|---|---|---|
| dailyOrders | 日均单量 | `a-input-number` | 必填，>0 | API 默认值 |
| itemsPerOrder | 件单比 | `a-input-number` | 必填，>0 | API 默认值 |
| workDays | 工作天数 | `a-input-number` | 必填，1-31 | 22 |
| laborEfficiency | 人效(件/人时) | `a-input-number` | 必填，>0 | API 默认值 |
| fixedLaborPrice | 固定劳务单价(元/h) | `a-input-number` | 必填，>0 | API 默认值 |
| tempLaborPrice | 临时劳务单价(元/h) | `a-input-number` | 必填，>0 | API 默认值 |
| fixedLaborRatio | 固临比(0-1) | `a-input-number` | 必填，0-1 | API 默认值 |
| taxRate | 税率 | `a-input-number` | 必填，0-1 | 0.06 |

**默认值**: 页面加载时调用 `getEstimateDefaults(warehouseCode)` 获取历史默认参数填充表单。

**提交**: 表单校验通过后调用 `calculate(request)` → 返回 `EstimateResultVO`。

#### Scenario: 表单初始化
- **WHEN** 用户进入成本估算页面
- **THEN** 表单 SHALL 调用 `getEstimateDefaults` API 并用历史默认值填充各字段

#### Scenario: 参数校验
- **WHEN** 用户提交表单但某必填字段为空或超出范围
- **THEN** SHALL 展示对应字段的校验错误提示，不发送 API 请求

#### Scenario: 提交计算
- **WHEN** 用户填写完参数并点击"计算"按钮
- **THEN** SHALL 调用 `calculate` API 并展示结果

#### Scenario: 切换仓库重载默认值
- **WHEN** 用户在顶栏切换仓库
- **THEN** 表单 SHALL 重新调用 `getEstimateDefaults` 加载新仓库的默认参数

### Requirement: 实时计算结果展示

**所属服务**: frontend  
**文件**: `src/views/EstimateView.vue`

表单提交后 SHALL 在右侧（或下方）展示计算结果卡片。

**结果展示**（对应 `EstimateResultVO`）:
| 指标 | 字段 | 格式 |
|---|---|---|
| 预估人数 | estimatedHeadcount | 保留1位小数 + "人" |
| 预估总工时 | estimatedTotalHours | 保留1位小数 + "h" |
| 加权平均单价 | weightedUnitPrice | ¥保留2位/h |
| 月度费用 | monthlyFee | ¥保留2位 |
| 单均成本 | costPerOrder | ¥保留2位/单 |
| 件均成本 | costPerItem | ¥保留2位/件 |

**布局**: 使用 `a-descriptions` 组件或 `a-statistic` 卡片组展示。

#### Scenario: 展示计算结果
- **WHEN** `calculate` API 返回成功
- **THEN** SHALL 展示 6 项估算结果数据

#### Scenario: 无结果初始状态
- **WHEN** 用户尚未提交计算
- **THEN** 结果区域 SHALL 展示引导提示"请填写参数后点击计算"

### Requirement: 与历史数据对比

**所属服务**: frontend  
**文件**: `src/views/EstimateView.vue`

计算结果出来后 SHALL 展示一个对比柱状图（ECharts `BarChart`），将估算结果与历史基线数据进行对比。

**数据源**: 
- 估算值: `EstimateResultVO` 中的 `monthlyFee`、`costPerOrder`、`costPerItem`
- 历史值: 调用 `getWarehouseDetail(warehouseCode)` → `WarehouseDetailVO` 中的对应字段

**图表配置**:
- X 轴: 对比维度（月度费用、单均成本、件均成本）
- 两个 series: "估算值" vs "历史值"
- 启用 `TooltipComponent` + `LegendComponent`

#### Scenario: 展示历史对比图
- **WHEN** 计算结果返回后
- **THEN** SHALL 展示估算值与历史值的对比柱状图

#### Scenario: 无历史数据
- **WHEN** 当前仓库无历史基线数据
- **THEN** 仅展示估算值，图表中历史值 series 为空并标注"暂无历史数据"
