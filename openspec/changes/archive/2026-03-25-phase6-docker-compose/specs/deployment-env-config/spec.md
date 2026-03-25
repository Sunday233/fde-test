# deployment-env-config

**所属服务**: 跨服务（backend + analytics）

## ADDED Requirements

### Requirement: .env 文件创建

系统 SHALL 在 `wh-op-platform/.env` 中定义所有运行时环境变量。`.env` 文件 MUST 被 `.gitignore` 忽略，不得提交到版本控制。

`.env` 文件 SHALL 包含以下变量组：

**MySQL 连接**:
```
MYSQL_HOST=10.126.50.199
MYSQL_PORT=3306
MYSQL_DB=wh_op_baseline
MYSQL_USER=<实际用户名>
MYSQL_PASSWORD=<实际密码>
```

**服务间通信**:
```
ANALYTICS_BASE_URL=http://analytics:8000
```

**存储路径**:
```
SQLITE_PATH=/data/results.db
```

#### Scenario: .env 文件格式正确

- **WHEN** Docker Compose 读取 `.env` 文件
- **THEN** 所有 `KEY=VALUE` 对被正确解析，无语法错误

#### Scenario: .env 不在版本控制中

- **WHEN** 检查 `.gitignore` 文件
- **THEN** 包含 `.env` 规则，`git status` 不显示 `.env` 文件

### Requirement: .env.example 同步

`.env.example` SHALL 与 `.env` 保持变量名一致，值使用占位符。当 `.env` 新增变量时，`.env.example` MUST 同步更新。

#### Scenario: .env.example 覆盖所有变量

- **WHEN** 对比 `.env` 和 `.env.example` 的变量名列表
- **THEN** `.env.example` 包含 `.env` 中的所有变量名，值为示例占位符

### Requirement: 敏感信息保护

`.env` 中的 `MYSQL_PASSWORD` 和 `MYSQL_USER` SHALL 不出现在以下位置：
- 代码仓库中的任何已提交文件
- Docker 镜像层中
- 日志输出中

#### Scenario: 密码不泄露到镜像

- **WHEN** 构建 Docker 镜像
- **THEN** `docker history` 中不包含任何环境变量值（环境变量在运行时注入，非构建时）
