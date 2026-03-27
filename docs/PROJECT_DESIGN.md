# wh-op-platform — 科捷仓内操作费用基线分析平台

## 项目设计文档 v1.0

**创建日期**: 2026-03-25  
**项目类型**: Monorepo 前后端分离 Web 应用  
**部署方式**: Docker Compose 单机部署  

---

## 一、需求总结

### 1.1 业务背景

COO 给科捷的经营管理部门布置的课题：精细化统计仓内操作费用的基线和影响因素，建立费用基线的相关算数应用。

- **核心用途**: 估算新业务成本和报价、复盘旧业务、设定仓经理效率指标
- **费用定义**: 操作费用 = 工时数 × 劳务单价（含固定劳务、长期劳务外包、临时劳务外包）
- **潜在影响因素**: SKU、单量、件单比、劳务单价、固临比、行业、仓面积、人数、工时数、系统及操作流程（人工赋值0-9）、自动化设备（人工赋值0-9）、仓经理技能（人工赋值1-5）

### 1.2 数据现状

- **数据源**: MySQL 5.7 @ 10.126.50.199:3306，库名 `wh_op_baseline`
- **19 张表**: 覆盖科捷 2 个仓（天津武清佩森A仓、常熟高新正创B仓）的 2025-03 和 2025-08 数据
- **当前规模**: 约 700 万行（最大表：仓位信息表 206 万行、工作量统计操作明细表 212 万行）
- **未来扩展**: 10-20 个仓库 × 1 年数据，规模将显著增长
- **直连模式**: 所有服务直连远端 MySQL，不在本地部署数据源数据库
- **性能策略**: 第一版 MVP 先用 MySQL 跑通，后续根据数据量增长情况引入 OLAP 引擎

### 1.3 已有分析成果（可复用）

- 数据 Profiling 报告（19张表的字段分析）
- 仓库类型划分与日均费用基线分析
- 科捷仓内操作费用基线分析报告（含影响因素量化、Pearson相关性分析）
- Python 分析脚本（cost_analysis.py、warehouse_type_analysis.py）

---

## 二、技术选型

| 层级 | 技术栈 | 版本/说明 |
|---|---|---|
| **前端** | Vue 3 + Vite | TypeScript, SFC |
| **UI 框架** | Ant Design Vue + Tailwind.css | 4.x | 
| **图表库** | ECharts | vue-echarts 封装 |
| **后端** | Spring Boot 3 | Java 21, Maven |
| **ORM** | MyBatis-Plus | 适合复杂统计查询 |
| **数据分析服务** | Python FastAPI | 独立微服务，提供分析算法 API |
| **数据源** | MySQL 5.7 | 直连远端，只读 |
| **结果存储** | SQLite | Docker 内轻量存储预计算结果 |
| **容器化** | Docker Compose | 单机部署 |
| **认证** | 无 | 第一版不做认证 |
| **权限** | 无 | 单一管理员角色 |

---

## 三、系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                   Docker Compose                     │
│                                                      │
│  ┌──────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │ Frontend │   │   Backend    │   │  Analytics   │ │
│  │ (Nginx)  │   │ (Spring Boot)│   │  (FastAPI)   │ │
│  │ :80      │   │ :8080        │   │  :8000       │ │
│  └────┬─────┘   └──────┬───────┘   └──────┬───────┘ │
│       │                │                   │         │
│       │    ┌───────────┴───────────────────┘         │
│       │    │                                         │
│       │    │  ┌─────────────┐                        │
│       │    │  │   SQLite    │ (预计算结果)            │
│       │    │  │  (volume)   │                        │
│       │    │  └─────────────┘                        │
└───────┼────┼─────────────────────────────────────────┘
        │    │
        │    │  ┌──────────────────────────┐
        │    └──│  MySQL 5.7 (远端)        │
        │       │  10.126.50.199:3306      │
        │       │  wh_op_baseline          │
        └───────┘  (19张原始数据表)         │
                └──────────────────────────┘
```

### 3.2 服务职责

#### Frontend (Vue 3 + Vite + Ant Design Vue)
- 数据看板 Dashboard（KPI 概览、趋势图、实时指标）
- 费用基线分析（按仓库/月份/类型查看费用基线）
- 影响因素分析（展示预计算的相关性分析结果、可视化）
- 成本估算/报价模拟（基于固定公式的计算器）
- 报告生成（Markdown/HTML 在线报告渲染与下载）

#### Backend (Spring Boot 3)
- RESTful API 网关，聚合数据查询和分析服务结果
- 直连 MySQL 查询原始业务数据
- 费用计算引擎（固定公式：操作费用 = 工时数 × 劳务单价 × (1+税率)）
- 报告生成服务（Markdown 模板 + 数据填充）
- 调用 Python 分析服务获取分析结果
- 读取 SQLite 预计算结果（或通过 Python 服务代理）

#### Analytics (Python FastAPI)
- 数据分析算法服务（Pearson 相关性、回归分析等）
- 费用基线计算
- 影响因素量化分析
- 预计算任务调度（定时更新分析结果写入 SQLite）
- 直连 MySQL 读取原始数据

### 3.3 数据流

```
[MySQL 原始数据] ──读取──▶ [Python FastAPI] ──预计算──▶ [SQLite 结果表]
                    │                                        │
                    ▼                                        ▼
              [Spring Boot] ◀────── 读取结果 ─────────────────┘
                    │
              [RESTful API]
                    │
                    ▼
              [Vue 前端]
```

---

## 四、Monorepo 目录结构

```
wh-op-platform/
├── README.md                        # 项目说明、编译、部署、启动命令
├── docker-compose.yml               # 统一编排
├── .gitignore
│
├── frontend/                        # Vue 3 前端
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── Dockerfile
│   ├── nginx.conf                   # Nginx 配置（反向代理 API）
│   ├── index.html
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/
│       │   └── index.ts             # 路由配置
│       ├── layouts/
│       │   └── MainLayout.vue       # 主布局（侧边栏+内容区）
│       ├── views/
│       │   ├── Dashboard.vue        # 数据看板
│       │   ├── CostBaseline.vue     # 费用基线分析
│       │   ├── ImpactAnalysis.vue   # 影响因素分析
│       │   ├── CostEstimator.vue    # 成本估算/报价模拟
│       │   └── ReportView.vue       # 报告查看/生成
│       ├── components/
│       │   ├── charts/              # ECharts 图表组件
│       │   │   ├── TrendChart.vue
│       │   │   ├── CorrelationChart.vue
│       │   │   ├── BarCompare.vue
│       │   │   └── HeatmapChart.vue
│       │   ├── KpiCard.vue          # KPI 卡片
│       │   └── ReportRenderer.vue   # Markdown 报告渲染
│       ├── api/
│       │   └── index.ts             # Axios API 封装
│       ├── stores/                  # Pinia 状态管理
│       │   └── app.ts
│       ├── types/
│       │   └── index.ts             # TypeScript 类型定义
│       └── utils/
│           └── format.ts            # 格式化工具
│
├── backend/                         # Spring Boot 3 后端
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/com/kejie/whop/
│           │   ├── WhOpApplication.java
│           │   ├── config/
│           │   │   ├── DataSourceConfig.java    # MySQL 数据源
│           │   │   ├── CorsConfig.java          # 跨域配置
│           │   │   └── WebConfig.java
│           │   ├── controller/
│           │   │   ├── DashboardController.java
│           │   │   ├── CostBaselineController.java
│           │   │   ├── ImpactAnalysisController.java
│           │   │   ├── CostEstimateController.java
│           │   │   └── ReportController.java
│           │   ├── service/
│           │   │   ├── DashboardService.java
│           │   │   ├── CostBaselineService.java
│           │   │   ├── CostEstimateService.java
│           │   │   ├── ReportService.java
│           │   │   └── AnalyticsClient.java     # 调用 Python 服务
│           │   ├── mapper/
│           │   │   ├── OutboundMapper.java       # 出库单
│           │   │   ├── InboundMapper.java        # 入库单
│           │   │   ├── AttendanceMapper.java     # 出勤统计
│           │   │   ├── QuotationMapper.java      # 报价信息
│           │   │   ├── WorkloadMapper.java       # 工作量统计
│           │   │   ├── ShelfMapper.java          # 上架单
│           │   │   ├── ReturnMapper.java         # 退货信息
│           │   │   └── InventoryMapper.java      # 仓位库存
│           │   ├── model/
│           │   │   ├── entity/                   # 数据库实体
│           │   │   ├── dto/                      # 数据传输对象
│           │   │   └── vo/                       # 视图对象
│           │   └── util/
│           │       └── CostFormula.java          # 费用计算公式
│           └── resources/
│               ├── application.yml
│               ├── mapper/                       # MyBatis XML
│               └── templates/                    # 报告模板
│                   └── report-template.md
│
├── analytics/                       # Python FastAPI 分析服务
│   ├── pyproject.toml               # 依赖管理
│   ├── Dockerfile
│   ├── requirements.txt
│   └── src/
│       ├── main.py                  # FastAPI 入口
│       ├── config.py                # 配置（DB连接、SQLite路径）
│       ├── db/
│       │   ├── mysql_client.py      # MySQL 连接
│       │   └── sqlite_client.py     # SQLite 结果存储
│       ├── routers/
│       │   ├── baseline.py          # 费用基线分析 API
│       │   ├── impact.py            # 影响因素分析 API
│       │   └── health.py            # 健康检查
│       ├── services/
│       │   ├── baseline_service.py  # 费用基线计算
│       │   ├── impact_service.py    # 影响因素量化分析
│       │   ├── correlation.py       # Pearson 相关性
│       │   └── precompute.py        # 预计算任务
│       ├── models/
│       │   └── schemas.py           # Pydantic 数据模型
│       └── tasks/
│           └── scheduler.py         # 定时预计算调度
│
├── data/                            # 数据目录（Git 忽略）
│   └── results.db                   # SQLite 预计算结果
│
└── docs/                            # 文档
    ├── data_profiling_reports/      # 现有 Profiling 报告
    └── data_analysis_reports/       # 现有分析报告
```

---

## 五、核心功能模块设计

### 5.1 数据看板 (Dashboard)

**KPI 卡片**:
- 总出库单量 / 总出库件数
- 总工时 / 人均产出
- 月度操作费用估算
- 固临比

**图表**:
- 日出库单量趋势折线图（按仓库对比）
- 月度费用构成柱状图
- 操作类型工作量分布饼图
- 双仓对比雷达图

### 5.2 费用基线分析

**功能**:
- 按仓库 / 月份 / 费用类型筛选
- 每月日均费用基线展示
- 不同类型仓库的费用对比
- 劳务单价分析（固定/临时/叉车/学生工）

**数据来源**: Spring Boot 直连 MySQL 聚合查询

### 5.3 影响因素分析

**功能**:
- 影响因素重要性排序（柱状图）
- Pearson 相关系数矩阵热力图
- 单因素散点图（选择因素 vs 工时/费用）
- 双仓对比分析

**数据来源**: Python 预计算结果（存 SQLite），前端展示

### 5.4 成本估算/报价模拟

**固定公式**:
$$
\text{月度操作费用} = \text{预估月度总工时(h)} \times \text{加权平均单价(元/h)} \times (1 + \text{税率})
$$

**输入参数**:
| 参数 | 说明 | 默认值来源 |
|---|---|---|
| 预估月度出库单量 | 用户输入 | 历史平均值 |
| 预估件单比 | 用户输入 | 历史平均值 |
| 人均产出（单/人/天） | 用户输入/历史值 | 历史平均值 |
| 工作天数 | 用户输入 | 默认 26 |
| 加权平均单价 | 用户选择仓库自动填充 | 报价表计算 |
| 税率 | 固定 | 6% |

**输出**:
- 预估所需人数
- 预估总工时
- 预估月度操作费用
- 单均成本 / 件均成本

### 5.5 报告生成

**功能**:
- 选择仓库和时间范围
- 自动生成 Markdown 格式分析报告
- 包含图表数据、分析结论
- 在线渲染预览 + 下载

---

## 六、API 设计概览

### Spring Boot API (`:8080`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/dashboard/overview` | 看板概览数据 |
| GET | `/api/dashboard/trend` | 趋势数据（按日/月聚合） |
| GET | `/api/baseline/monthly` | 月度费用基线 |
| GET | `/api/baseline/warehouse/{id}` | 单仓详情 |
| GET | `/api/baseline/compare` | 双仓对比 |
| GET | `/api/impact/factors` | 影响因素分析结果 |
| GET | `/api/impact/correlation` | 相关性矩阵 |
| POST | `/api/estimate/calculate` | 成本估算计算 |
| GET | `/api/estimate/defaults/{warehouseId}` | 获取默认参数值 |
| POST | `/api/report/generate` | 生成报告 |
| GET | `/api/report/list` | 报告列表 |
| GET | `/api/report/{id}` | 获取报告内容 |
| GET | `/api/warehouses` | 仓库列表 |

### Python FastAPI (`:8000`)

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| POST | `/api/analyze/baseline` | 触发费用基线分析 |
| POST | `/api/analyze/impact` | 触发影响因素分析 |
| GET | `/api/results/baseline` | 获取基线分析结果 |
| GET | `/api/results/impact` | 获取影响因素结果 |
| GET | `/api/results/correlation` | 获取相关性矩阵 |
| POST | `/api/precompute/trigger` | 手动触发预计算 |

---

## 七、Docker Compose 编排

```yaml
# docker-compose.yml 概要设计
version: "3.9"

services:
  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - MYSQL_HOST=10.126.50.199
      - MYSQL_PORT=3306
      - MYSQL_DB=wh_op_baseline
      - MYSQL_USER=fdeuser
      - MYSQL_PASSWORD=FDE2026!
      - ANALYTICS_URL=http://analytics:8000
    depends_on:
      - analytics

  analytics:
    build: ./analytics
    ports:
      - "8000:8000"
    environment:
      - MYSQL_HOST=10.126.50.199
      - MYSQL_PORT=3306
      - MYSQL_DB=wh_op_baseline
      - MYSQL_USER=fdeuser
      - MYSQL_PASSWORD=FDE2026!
      - SQLITE_PATH=/data/results.db
    volumes:
      - analytics-data:/data

volumes:
  analytics-data:
```

> **安全提醒**: 生产环境应使用 Docker Secrets 或 .env 文件管理敏感信息，不要在 docker-compose.yml 中硬编码密码。

---

## 八、Nginx 反向代理配置

```nginx
# frontend/nginx.conf 概要
server {
    listen 80;

    # 前端静态资源
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # Spring Boot API 代理
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 九、预计算结果 SQLite 表设计

```sql
-- 费用基线预计算结果
CREATE TABLE baseline_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id TEXT NOT NULL,
    warehouse_name TEXT,
    month TEXT NOT NULL,            -- '2025-03'
    outbound_orders INTEGER,
    outbound_items REAL,
    avg_items_per_order REAL,       -- 件单比
    total_work_hours REAL,
    headcount INTEGER,
    fixed_count INTEGER,
    temp_count INTEGER,
    fixed_temp_ratio REAL,          -- 固临比
    avg_unit_price REAL,            -- 加权平均单价(元/h)
    estimated_cost REAL,            -- 估算劳务费
    cost_per_order REAL,            -- 单均成本
    cost_per_item REAL,             -- 件均成本
    productivity REAL,              -- 人效(单/人/天)
    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 影响因素分析结果
CREATE TABLE impact_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id TEXT NOT NULL,
    warehouse_name TEXT,
    factor_name TEXT NOT NULL,       -- 影响因素名称
    correlation_r REAL,              -- Pearson 相关系数
    direction TEXT,                  -- '正' / '负'
    strength TEXT,                   -- '强' / '中等' / '弱'
    rank_order INTEGER,              -- 重要性排序
    sample_count INTEGER,            -- 有效样本数
    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 日维度明细（用于散点图等）
CREATE TABLE daily_metrics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    warehouse_id TEXT NOT NULL,
    date TEXT NOT NULL,              -- 'YYYY-MM-DD'
    outbound_orders INTEGER,
    outbound_items REAL,
    items_per_order REAL,
    inbound_orders INTEGER,
    return_orders INTEGER,
    shelf_orders INTEGER,
    headcount INTEGER,
    fixed_count INTEGER,
    temp_count INTEGER,
    own_count INTEGER,
    total_work_hours REAL,
    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 十、关键设计决策说明

| 决策 | 选择 | 理由 |
|---|---|---|
| 直连远端 MySQL | 不做本地数据同步 | 简化架构，当前数据量可承受 |
| SQLite 存结果 | 不存 MySQL | 预计算结果轻量、Docker 内自给自足 |
| 影响因素预计算 | 非实时计算 | 相关性分析计算量大，无需实时 |
| 费用公式硬编码 | 非可配置 | 当前公式固定，避免过度设计 |
| 无认证 | 第一版跳过 | 内部分析工具，快速交付 MVP |
| FastAPI 独立服务 | 非 Spring Boot 内嵌 Python | 职责分离，Python 生态更适合数据分析 |
| MyBatis-Plus | 非 JPA | 复杂统计查询场景多，MyBatis 更灵活 |
| 架构预留 OLAP 扩展 | 第一版不引入 | 数据访问层抽象，后续可切换 |

---

## 十一、后续扩展路线

1. **OLAP 引擎**: 当数据量达到亿级时引入 ClickHouse/Doris，通过 ETL 同步 MySQL 数据
2. **认证鉴权**: 按需引入 Spring Security + JWT
3. **RBAC**: 多角色支持（管理员、分析师、查看者）
4. **数据导入**: 支持 Excel/CSV 上传补充数据
5. **机器学习模型**: 回归模型辅助费用预测
6. **仓库画像**: 自动化仓库类型标签系统
