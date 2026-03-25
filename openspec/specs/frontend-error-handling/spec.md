# frontend-error-handling

**所属服务**: frontend

## ADDED Requirements

### Requirement: 页面级错误状态 UI

每个数据页面（DashboardView、BaselineView、ImpactView、EstimateView、ReportView）SHALL 在 API 请求失败时显示错误状态卡片，包含错误描述文字和「重试」按钮。点击重试 SHALL 重新调用页面数据加载函数。

错误状态 SHALL 替代当前的空白/loading 状态，而非叠加显示。

#### Scenario: Dashboard API 失败显示错误 UI

- **WHEN** DashboardView 的任一 API 请求失败（网络错误或 HTTP 非 200）
- **THEN** 页面显示包含「数据加载失败」文字和「重试」按钮的错误卡片
- **WHEN** 用户点击「重试」按钮
- **THEN** 页面重新发起 API 请求，成功后恢复正常数据展示

#### Scenario: 错误状态与正常状态互斥

- **WHEN** 页面处于错误状态且用户点击重试后请求成功
- **THEN** 错误卡片消失，正常数据和图表显示

### Requirement: 增强空数据提示

当 API 返回空数据（`records.length === 0`）时，各页面 SHALL 显示包含图标和描述文字的空状态提示（使用 `a-empty` 组件），替代当前简单的文字提示。

#### Scenario: 表格无数据显示空状态组件

- **WHEN** BaselineView 查询返回 0 条记录
- **THEN** 表格区域显示 `a-empty` 组件，包含「暂无基线数据」描述文字

### Requirement: 请求取消

每个 View SHALL 在组件卸载时（`onUnmounted`）取消所有进行中的 API 请求，使用 `AbortController` + Axios `signal` 参数。

#### Scenario: 快速切换页面取消旧请求

- **WHEN** 用户在 DashboardView 数据加载过程中切换到 BaselineView
- **THEN** DashboardView 的进行中请求被取消（AbortController.abort()），不会触发错误 toast

#### Scenario: 取消的请求不触发错误提示

- **WHEN** 请求因 AbortController 被取消
- **THEN** Axios 拦截器识别 `CanceledError`，不显示错误 toast

### Requirement: Axios 拦截器增强

Axios 响应拦截器 SHALL 区分以下错误类型：
- 请求取消（`axios.isCancel`）：静默处理，不显示 toast
- 网络超时（`error.code === 'ECONNABORTED'`）：显示「请求超时，请检查网络连接」
- 服务端错误（HTTP 5xx）：显示「服务器繁忙，请稍后重试」
- 业务错误（code ≠ 200）：显示 `result.message`（当前行为保持不变）

#### Scenario: 请求超时显示专用提示

- **WHEN** API 请求超过 30 秒未响应
- **THEN** 页面显示「请求超时，请检查网络连接」toast

#### Scenario: 取消的请求静默处理

- **WHEN** 请求被 AbortController 取消
- **THEN** 不显示任何错误 toast
