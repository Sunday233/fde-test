# frontend-responsive-layout

**所属服务**: frontend

## ADDED Requirements

### Requirement: Dashboard KPI 卡片响应式

DashboardView 的 4 张 KPI 卡片 SHALL 使用 Ant Design Grid 响应式属性：
- `xs="24"`: 单列（手机）
- `sm="12"`: 双列（平板）
- `lg="6"`: 四列（桌面）

#### Scenario: 手机端 KPI 卡片单列显示

- **WHEN** 浏览器宽度 < 576px
- **THEN** 4 张 KPI 卡片纵向排列，每张占满整行

#### Scenario: 桌面端 KPI 卡片四列显示

- **WHEN** 浏览器宽度 ≥ 992px
- **THEN** 4 张 KPI 卡片水平排列在同一行

### Requirement: 双栏图表响应式

所有包含 `a-col :span="12"` 双栏并排布局的图表区域（Dashboard 图表、Baseline 对比图、Impact 双仓对比、Estimate 表单+结果）SHALL 在小屏下堆叠为单列：
- `xs="24"`: 单列
- `md="12"`: 双列

#### Scenario: 平板端双栏堆叠为单列

- **WHEN** 浏览器宽度 < 768px
- **THEN** 双栏并排的图表区域变为上下堆叠，每个图表占满整行

### Requirement: 侧边栏移动端自动折叠

`MainLayout.vue` 的 `a-layout-sider` SHALL 在浏览器宽度 ≤ 768px 时默认折叠（`collapsed = true`）。使用 `a-layout-sider` 的 `breakpoint="md"` 属性实现自动响应。

#### Scenario: 移动端侧边栏默认折叠

- **WHEN** 浏览器宽度 ≤ 768px
- **THEN** 侧边栏自动折叠为图标模式，用户可手动展开

#### Scenario: 桌面端侧边栏默认展开

- **WHEN** 浏览器宽度 > 768px
- **THEN** 侧边栏保持展开状态

### Requirement: Header 仓库选择器自适应

`MainLayout.vue` 中的仓库选择器（`a-select`）SHALL 宽度自适应：
- 桌面端：`width: 240px`（当前值）
- 移动端（≤ 768px）：`width: 160px`

#### Scenario: 移动端选择器不溢出

- **WHEN** 浏览器宽度 ≤ 768px
- **THEN** 仓库选择器宽度缩小为 160px，不超出 Header 区域

### Requirement: 图表高度自适应

所有 VChart 组件的容器高度 SHALL 在移动端（< 576px）适当缩小，从 320px 调整为 240px，避免小屏幕上图表占据过多垂直空间。

#### Scenario: 移动端图表高度缩小

- **WHEN** 浏览器宽度 < 576px
- **THEN** 图表容器高度为 240px（较桌面端 320px 缩小）
