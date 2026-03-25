# vue-project-skeleton

**所属服务**: frontend

## ADDED Requirements

### Requirement: Vite + Vue 3 + TypeScript 项目初始化

系统 SHALL 在 `wh-op-platform/frontend/` 目录中初始化一个基于 Vite 6 + Vue 3.5 + TypeScript 的前端项目。

项目结构：
```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vite.config.ts
├── env.d.ts
├── public/
│   └── favicon.ico
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/           # API 请求层
    ├── assets/        # 静态资源
    ├── components/    # 公共组件
    ├── layouts/       # 布局组件
    ├── plugins/       # 插件配置
    ├── router/        # 路由配置
    ├── stores/        # Pinia store
    ├── types/         # TypeScript 类型
    └── views/         # 页面视图
```

`tsconfig.json` MUST 启用 TypeScript 严格模式（`"strict": true`）。

`vite.config.ts` MUST 配置路径别名 `@` 指向 `src/` 目录。

#### Scenario: 项目初始化后可正常启动

- **WHEN** 执行 `npm install && npm run dev`
- **THEN** Vite 开发服务器正常启动，浏览器访问 `http://localhost:5173` 显示默认页面，无编译错误

#### Scenario: TypeScript 严格模式生效

- **WHEN** 在 `.vue` 或 `.ts` 文件中使用隐式 `any` 类型
- **THEN** TypeScript 编译器报错

### Requirement: Ant Design Vue 4.x 按需加载配置

系统 SHALL 安装 `ant-design-vue@4` 并通过 `unplugin-vue-components` + `@ant-design-vue/resolver` 实现组件按需自动导入，无需手动 import。

`vite.config.ts` 中 MUST 配置 `Components` 插件：
```ts
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'

Components({
  resolvers: [AntDesignVueResolver({ importStyle: false })]
})
```

#### Scenario: Ant Design Vue 组件按需使用

- **WHEN** 在模板中直接使用 `<a-button>` 标签
- **THEN** 组件自动解析并渲染，无需在 `<script>` 中手动 import

### Requirement: Tailwind CSS 4 配置

系统 SHALL 安装 `tailwindcss@4` 和 `@tailwindcss/vite` 插件。

`vite.config.ts` 中 MUST 注册 Tailwind CSS Vite 插件。

`src/assets/main.css` MUST 引入 Tailwind：
```css
@import "tailwindcss";
```

Tailwind 4 的 preflight MUST 被禁用以避免覆盖 Ant Design Vue 基础样式：
```css
@layer base {
  @import "tailwindcss/preflight" layer(base);
}
```
或在 CSS 中使用 `@preflight false` 禁用。

#### Scenario: Tailwind 工具类正常生效

- **WHEN** 在模板中使用 `<div class="flex gap-4 p-2">`
- **THEN** 对应的 flex、gap、padding 样式正确应用

#### Scenario: Tailwind 不影响 Ant Design 样式

- **WHEN** 使用 Ant Design Vue 的 `<a-button type="primary">` 组件
- **THEN** 按钮样式与 Ant Design 默认主题一致，不受 Tailwind preflight 影响

### Requirement: vue-echarts 7 + echarts 5 按需引入配置

系统 SHALL 安装 `vue-echarts@7` 和 `echarts@5`，并在 `src/plugins/echarts.ts` 中集中注册需要的 echarts 模块。

初始注册的 echarts 模块：
- `use([CanvasRenderer])` — 渲染器
- `BarChart` — 柱状图
- `LineChart` — 折线图
- `PieChart` — 饼图
- `ScatterChart` — 散点图
- `HeatmapChart` — 热力图
- `GridComponent` — 网格
- `TooltipComponent` — 提示框
- `LegendComponent` — 图例
- `TitleComponent` — 标题
- `ToolboxComponent` — 工具箱
- `VisualMapComponent` — 视觉映射（热力图颜色）

`src/main.ts` MUST 导入 `plugins/echarts.ts` 以激活注册。

#### Scenario: ECharts 组件可用

- **WHEN** 在 Vue 组件中使用 `<v-chart :option="chartOption" autoresize />`
- **THEN** 图表正常渲染，支持柱状图、折线图、饼图、散点图、热力图

### Requirement: Pinia 状态管理初始化

系统 SHALL 安装 `pinia@2`，在 `src/main.ts` 中创建并注册 Pinia 实例。

系统 SHALL 创建 `src/stores/app.ts`，导出 `useAppStore`，包含初始状态：
- `currentWarehouse: string | null` — 当前选中的仓库编码

#### Scenario: Pinia store 挂载成功

- **WHEN** 应用启动后在任意组件中调用 `useAppStore()`
- **THEN** 返回响应式 store 实例，`currentWarehouse` 初始值为 `null`

### Requirement: 入口文件配置

`src/main.ts` MUST 按以下顺序初始化：
1. 导入 `assets/main.css`（Tailwind + 全局样式）
2. 导入 `plugins/echarts.ts`（ECharts 模块注册）
3. 创建 Vue App（`createApp(App)`）
4. 注册 Pinia（`app.use(createPinia())`）
5. 注册 Vue Router（`app.use(router)`）
6. 挂载到 `#app`（`app.mount('#app')`）

`src/App.vue` MUST 仅包含 `<RouterView />`，不含任何布局逻辑。

#### Scenario: 应用正常挂载

- **WHEN** 浏览器加载 `index.html`
- **THEN** Vue 应用挂载到 `#app`，Router 和 Pinia 均可用，控制台无报错
