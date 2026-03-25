## ADDED Requirements

### Requirement: 报告列表

**所属服务**: frontend  
**文件**: `src/views/ReportView.vue`

页面 SHALL 展示已生成报告的列表，使用 `a-table` 组件。

**数据源**: 调用 `getReportList()` → `ReportVO[]`

**表格列定义**:
| 列标题 | 字段 | 格式 |
|---|---|---|
| 报告标题 | title | 文本，可点击跳转预览 |
| 仓库 | warehouseName | 文本 |
| 开始月份 | startMonth | YYYY-MM |
| 结束月份 | endMonth | YYYY-MM |
| 生成时间 | createdAt | YYYY-MM-DD HH:mm |
| 操作 | — | "预览" + "下载" 按钮 |

**功能**: 支持分页，点击"预览"展开 Markdown 渲染内容，点击"下载"触发 HTML 下载。

#### Scenario: 展示报告列表
- **WHEN** 用户进入报告页面
- **THEN** SHALL 调用 `getReportList` 并展示报告表格

#### Scenario: 列表为空
- **WHEN** 无已生成报告
- **THEN** 表格 SHALL 展示空状态，引导用户生成报告

### Requirement: 报告生成表单

**所属服务**: frontend  
**文件**: `src/views/ReportView.vue`

页面 SHALL 提供报告生成表单（`a-card` + `a-form`），放置在列表上方或以 `a-modal` 弹窗形式展示。

**表单字段**（对应 `ReportGenerateRequest`）:
| 字段 | 标签 | 组件 | 校验 |
|---|---|---|---|
| warehouseCode | 仓库 | `a-select` | 必填 |
| startMonth | 开始月份 | `a-date-picker` (picker="month") | 必填 |
| endMonth | 结束月份 | `a-date-picker` (picker="month") | 必填，≥ startMonth |

**提交**: 校验通过后调用 `generateReport(request)` → 返回新 `ReportVO`，成功后刷新列表并自动预览新报告。

#### Scenario: 生成报告
- **WHEN** 用户填写仓库和时间范围后点击"生成报告"
- **THEN** SHALL 调用 `generateReport` API，成功后列表刷新且自动展示新报告预览

#### Scenario: 校验失败  
- **WHEN** 用户未填写必填字段或结束月份早于开始月份
- **THEN** SHALL 展示校验错误提示，不发送请求

#### Scenario: 生成中状态
- **WHEN** 报告正在生成中
- **THEN** 提交按钮 SHALL 展示 loading 状态，防止重复提交

### Requirement: Markdown 在线渲染预览

**所属服务**: frontend  
**文件**: `src/views/ReportView.vue`

点击报告的"预览"按钮后 SHALL 展示 Markdown 内容的 HTML 渲染结果。

**实现方式**:
- 调用 `getReportDetail(id)` 获取报告详情（`ReportVO` 需扩展 `content: string` 字段存放 Markdown 文本）
- 使用 `markdown-it` 库将 Markdown 转为 HTML
- 渲染区域使用 `v-html` 展示，外层 `a-card` 包裹

**安全**: `markdown-it` 实例 SHALL 配置 `html: false` 以防止 XSS 注入。

**展示方式**: 使用 `a-drawer`（从右侧滑出）或页面内折叠区域展示。

#### Scenario: 预览报告
- **WHEN** 用户点击某报告的"预览"按钮
- **THEN** SHALL 调用 `getReportDetail` 获取内容并渲染为 HTML 展示在抽屉面板中

#### Scenario: 内容安全
- **WHEN** Markdown 内容包含 HTML 标签
- **THEN** `markdown-it` SHALL 不渲染原始 HTML 标签（`html: false`）

### Requirement: 报告下载（HTML 格式）

**所属服务**: frontend  
**文件**: `src/views/ReportView.vue`

点击"下载"按钮 SHALL 将报告导出为 HTML 文件并触发浏览器下载。

**实现方式**:
1. 获取 Markdown 内容（复用 `getReportDetail`）
2. 使用 `markdown-it` 转为 HTML 片段
3. 包裹在完整 HTML 文档模板中（含 `<style>` 基础排版样式）
4. 创建 `Blob`（type: `text/html`）→ `URL.createObjectURL` → `<a>` 元素 click 触发下载
5. 下载完成后调用 `URL.revokeObjectURL` 释放内存

**文件名**: `{报告标题}_{仓库名}_{时间范围}.html`

#### Scenario: 下载 HTML 报告
- **WHEN** 用户点击某报告的"下载"按钮
- **THEN** 浏览器 SHALL 下载一个格式化的 HTML 文件

#### Scenario: 下载文件可独立打开
- **WHEN** 用户用浏览器打开下载的 HTML 文件
- **THEN** SHALL 展示排版良好的报告内容（含内联 CSS 样式）
