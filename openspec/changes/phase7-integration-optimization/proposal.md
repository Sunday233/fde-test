## Why

各服务模块已独立开发完成（前端 5 个页面、后端 11 个 API、Python 分析服务 3 阶段预计算），但尚未进行端到端联调验证。MySQL 查询在 700 万行数据下缺乏索引优化，前端无分页策略，错误处理仅有全局 toast 无重试机制，且布局仅适配桌面端。需要在集成部署前完成联调和性能/体验优化。

## What Changes

**前端 (frontend)**:
- 为所有数据表格添加后端分页支持（替代当前一次性加载全量数据）
- 在 API 拦截器中添加请求超时恢复和重试提示
- 为每个页面添加错误状态 UI（重试按钮）和空数据提示优化
- 添加响应式断点，使 Dashboard KPI 卡片、双栏图表在小屏下堆叠为单列
- 侧边栏在移动端默认折叠，Header 仓库选择器宽度自适应

**后端 (backend)**:
- 为高频聚合查询添加 MySQL 索引（出库单表、出勤统计表、报价信息表等）
- 为 `getMonthlyBaseline` 和 `getReportList` 添加分页参数支持
- 优化 Dashboard 聚合查询，减少全表扫描

**分析服务 (analytics)**:
- 验证预计算结果与前端展示的数据一致性
- 验证预计算调度器在容器环境中正常运行

### 非目标

- 不引入新的业务功能或新页面
- 不进行大规模架构重构（如引入 Redis 缓存、消息队列）
- 不涉及认证/权限系统
- 不做移动端 App 适配（仅浏览器响应式）

## Capabilities

### New Capabilities

- `mysql-query-optimization`: MySQL 索引策略与后端查询优化 — 为高频聚合查询创建复合索引，优化 MyBatis-Plus QueryWrapper
- `frontend-error-handling`: 前端错误处理与边界情况 — API 重试提示、错误状态 UI、空数据增强、请求取消
- `frontend-responsive-layout`: 前端响应式布局适配 — Ant Design Grid 断点配置、移动端侧边栏折叠、Header 自适应

### Modified Capabilities

（无需修改现有 spec 级别的需求，本次变更为实现层面的优化）

## Impact

- **后端**: `backend/src/main/java/com/kejie/whop/service/` 下的 Service 类查询逻辑优化；新增 SQL migration 脚本创建索引
- **前端**: 所有 5 个 View 组件（Dashboard/Baseline/Impact/Estimate/Report）的错误处理和响应式布局改造；`src/api/index.ts` 拦截器增强；`MainLayout.vue` 响应式调整
- **分析服务**: 仅验证，不修改代码
- **数据库**: MySQL `wh_op_baseline` 库新增 4-6 个索引（只读库，不影响写入性能）
