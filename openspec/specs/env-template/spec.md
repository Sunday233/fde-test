## ADDED Requirements

### Requirement: .env.example 环境变量模板

系统 SHALL 在 `wh-op-platform/` 根目录创建 `.env.example` 文件，定义所有服务所需的环境变量及说明注释，敏感信息使用占位符而非真实值。

所属服务：项目根目录（供 backend 和 analytics 服务使用）

#### Scenario: 包含 MySQL 连接配置
- **WHEN** 查看 .env.example
- **THEN** SHALL 包含 `MYSQL_HOST`（默认 `10.126.50.199`）
- **THEN** SHALL 包含 `MYSQL_PORT`（默认 `3306`）
- **THEN** SHALL 包含 `MYSQL_DB`（默认 `wh_op_baseline`）
- **THEN** SHALL 包含 `MYSQL_USER`（占位符，提示用户填写）
- **THEN** SHALL 包含 `MYSQL_PASSWORD`（占位符，提示用户填写）

#### Scenario: 包含服务配置
- **WHEN** 查看 .env.example
- **THEN** SHALL 包含 `ANALYTICS_URL`（默认 `http://analytics:8000`）
- **THEN** SHALL 包含 `SQLITE_PATH`（默认 `/data/results.db`）

#### Scenario: 包含注释说明
- **WHEN** 查看 .env.example
- **THEN** 每个变量 SHALL 有中文注释说明其用途
- **THEN** 敏感信息（用户名、密码）SHALL 使用占位符（如 `your_username`、`your_password`）
- **THEN** 文件头部 SHALL 有使用说明（复制为 .env 后修改）
