# frontend-routing

**所属服务**: frontend

## ADDED Requirements

### Requirement: Vue Router 配置

系统 SHALL 在 `src/router/index.ts` 中配置 Vue Router 4，使用 `createWebHistory` 历史模式。

路由表 MUST 包含以下 5 个一级路由，全部嵌套在 `MainLayout` 下：

| 路径 | 名称 | 组件 | 菜单标题 |
|---|---|---|---|
| `/` | Dashboard | `views/DashboardView.vue` | 数据看板 |
| `/baseline` | Baseline | `views/BaselineView.vue` | 费用基线 |
| `/impact` | Impact | `views/ImpactView.vue` | 影响因素 |
| `/estimate` | Estimate | `views/EstimateView.vue` | 成本估算 |
| `/report` | Report | `views/ReportView.vue` | 报告 |

路由结构：
```ts
{
  path: '/',
  component: MainLayout,
  children: [
    { path: '', name: 'Dashboard', component: DashboardView },
    { path: 'baseline', name: 'Baseline', component: BaselineView },
    { path: 'impact', name: 'Impact', component: ImpactView },
    { path: 'estimate', name: 'Estimate', component: EstimateView },
    { path: 'report', name: 'Report', component: ReportView },
  ]
}
```

子路由组件 MUST 使用懒加载（`() => import(...)`）：
```ts
component: () => import('@/views/DashboardView.vue')
```

#### Scenario: 默认路由渲染 Dashboard

- **WHEN** 用户访问 `/`
- **THEN** 路由匹配 `Dashboard`，内容区渲染 `DashboardView` 占位组件

#### Scenario: 路由懒加载

- **WHEN** 用户首次点击"影响因素"菜单项
- **THEN** `ImpactView.vue` 组件按需加载（网络请求中可见独立 chunk），无需首屏加载全部页面

#### Scenario: 不存在的路径

- **WHEN** 用户访问 `/unknown`
- **THEN** 路由不匹配任何子路由，显示空白内容区（Phase 5 可添加 404 页面）

### Requirement: 占位页面组件

系统 SHALL 为每个路由创建对应的占位 View 组件（`src/views/` 目录下），内容仅包含页面标题文字，作为 Phase 5 页面开发的起点。

每个占位组件结构：
```vue
<template>
  <div class="p-6">
    <h1>{{ 页面标题 }}</h1>
    <p>页面开发中...</p>
  </div>
</template>
```

需创建的 5 个占位组件：
- `DashboardView.vue` — 标题"数据看板"
- `BaselineView.vue` — 标题"费用基线分析"
- `ImpactView.vue` — 标题"影响因素分析"
- `EstimateView.vue` — 标题"成本估算"
- `ReportView.vue` — 标题"报告管理"

#### Scenario: 占位页面正确渲染

- **WHEN** 路由导航到 `/baseline`
- **THEN** 内容区显示"费用基线分析"标题和"页面开发中..."提示文字
