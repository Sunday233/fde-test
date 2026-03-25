## ADDED Requirements

### Requirement: .gitignore 规则配置

系统 SHALL 在 `wh-op-platform/` 根目录创建 `.gitignore` 文件，覆盖 Java/Maven、Node.js、Python 三种语言的构建产物和依赖目录，以及项目特有的忽略规则。

所属服务：项目根目录（覆盖 frontend / backend / analytics 三个服务）

#### Scenario: 忽略 Java/Maven 产物
- **WHEN** 后端 Spring Boot 项目构建
- **THEN** `.gitignore` SHALL 忽略 `target/`、`*.class`、`*.jar`、`*.war`
- **THEN** SHALL 忽略 IDE 文件 `.idea/`、`*.iml`

#### Scenario: 忽略 Node.js 产物
- **WHEN** 前端 Vue 项目构建
- **THEN** `.gitignore` SHALL 忽略 `node_modules/`、`dist/`、`.vite/`
- **THEN** SHALL 忽略日志文件 `*.log`、`npm-debug.log*`

#### Scenario: 忽略 Python 产物
- **WHEN** Python FastAPI 服务运行
- **THEN** `.gitignore` SHALL 忽略 `__pycache__/`、`*.pyc`、`*.pyo`
- **THEN** SHALL 忽略虚拟环境 `.venv/`、`venv/`、`env/`

#### Scenario: 忽略项目特有文件
- **WHEN** 项目运行产生数据文件和配置
- **THEN** `.gitignore` SHALL 忽略 `.env`（保留 `.env.example`）
- **THEN** SHALL 忽略 `data/*.db`（SQLite 数据文件）
- **THEN** SHALL 忽略 `.DS_Store`（macOS 系统文件）
