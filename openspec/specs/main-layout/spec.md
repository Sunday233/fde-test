# main-layout

**所属服务**: frontend

## ADDED Requirements

### Requirement: MainLayout 布局组件

系统 SHALL 创建 `src/layouts/MainLayout.vue` 作为应用主布局，使用 Ant Design Vue 的布局组件。

组件树结构：
```
<a-layout style="min-height: 100vh">
  <a-layout-sider collapsible v-model:collapsed="collapsed">
    <div class="logo">仓内操作费用分析</div>
    <a-menu theme="dark" mode="inline" v-model:selectedKeys="selectedKeys">
      <a-menu-item key="/">
        <DashboardOutlined /> <span>数据看板</span>
      </a-menu-item>
      <a-menu-item key="/baseline">
        <BarChartOutlined /> <span>费用基线</span>
      </a-menu-item>
      <a-menu-item key="/impact">
        <ExperimentOutlined /> <span>影响因素</span>
      </a-menu-item>
      <a-menu-item key="/estimate">
        <CalculatorOutlined /> <span>成本估算</span>
      </a-menu-item>
      <a-menu-item key="/report">
        <FileTextOutlined /> <span>报告</span>
      </a-menu-item>
    </a-menu>
  </a-layout-sider>
  <a-layout>
    <a-layout-header>
      <!-- 顶栏：仓库选择器 -->
    </a-layout-header>
    <a-layout-content>
      <RouterView />
    </a-layout-content>
  </a-layout>
</a-layout>
```

**数据流**:
1. 侧栏菜单 `selectedKeys` 与 `router.currentRoute.path` 绑定
2. 点击菜单项通过 `router.push(key)` 导航
3. 顶栏包含仓库选择下拉框（`<a-select>`），选中值写入 `useAppStore().currentWarehouse`

#### Scenario: 侧栏显示 5 个导航菜单项

- **WHEN** 应用加载后显示 MainLayout
- **THEN** 左侧导航栏显示 5 个菜单项：数据看板、费用基线、影响因素、成本估算、报告，每项带对应图标

#### Scenario: 菜单项点击导航

- **WHEN** 用户点击"费用基线"菜单项
- **THEN** 路由跳转到 `/baseline`，内容区渲染 `BaselineView` 组件，菜单项高亮

#### Scenario: 侧栏可折叠

- **WHEN** 用户点击侧栏底部的折叠按钮
- **THEN** 侧栏折叠为图标模式，仅显示菜单图标，内容区扩展填充空间

#### Scenario: 菜单选中状态与路由同步

- **WHEN** 用户通过浏览器地址栏直接访问 `/impact`
- **THEN** 侧栏"影响因素"菜单项高亮，内容区渲染 `ImpactView`

### Requirement: 顶栏仓库选择器

系统 SHALL 在顶栏右侧提供仓库选择下拉框（`<a-select>`），选项数据来源于 `GET /api/warehouses` 接口。

**数据流**:
1. MainLayout `onMounted` 时调用 `GET /api/warehouses` 获取仓库列表
2. 下拉框选项展示 `warehouseName`，值为 `warehouseCode`
3. 选中值写入 `useAppStore().currentWarehouse`
4. 默认选中列表中第一个仓库

#### Scenario: 仓库列表加载并选中默认值

- **WHEN** MainLayout 挂载完成
- **THEN** 顶栏仓库选择器加载仓库列表，默认选中第一个仓库（如"天津武清佩森A仓"）

#### Scenario: 切换仓库更新全局状态

- **WHEN** 用户在顶栏选择"常熟高新正创B仓"
- **THEN** `useAppStore().currentWarehouse` 更新为对应的 `warehouseCode`
