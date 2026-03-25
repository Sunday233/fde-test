## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: MySQL 只读连接池

系统 SHALL 提供 MySQL 只读连接模块 `db/mysql_client.py`，通过 pymysql 直连远端 MySQL 5.7。

**连接池实现**:
- 使用 `queue.Queue` 实现简单连接池，默认池大小 5
- 提供上下文管理器 `get_connection()` 自动借还连接
- 连接参数：charset=utf8mb4, cursorclass=DictCursor, connect_timeout=10
- 所有连接 MUST 设置 `read_only=True`（通过 `SET SESSION TRANSACTION READ ONLY`）
- 连接异常时自动重连（连接池检测到断开的连接时创建新连接）

**配置来源**: 从 `config.settings` 读取 MYSQL_HOST、MYSQL_PORT、MYSQL_USER、MYSQL_PASSWORD、MYSQL_DATABASE

**数据源**: MySQL 5.7 @ 10.126.50.199:3306, 数据库 `wh_op_baseline`, 19 张原始业务表

**安全约束**:
- 数据库密码 MUST 通过环境变量注入，不硬编码
- 所有查询 MUST 为只读（SELECT），不执行 INSERT/UPDATE/DELETE

#### Scenario: 成功获取数据库连接

- **WHEN** 调用 `get_connection()` 上下文管理器
- **THEN** 返回一个可用的 pymysql Connection 对象，可执行 `cursor.execute(SELECT ...)`，退出上下文时连接归还池中

#### Scenario: 连接池耗尽时阻塞等待

- **WHEN** 所有池内连接均被占用，新请求调用 `get_connection()`
- **THEN** 请求阻塞等待（最长 30 秒），超时后抛出异常

#### Scenario: MySQL 不可达时的错误处理

- **WHEN** MySQL 服务器不可达，调用 `get_connection()`
- **THEN** 抛出 `ConnectionError`，日志记录连接失败详情（不包含密码）
