## ADDED Requirements

**所属服务**: analytics (Python FastAPI)

### Requirement: Python 服务 Dockerfile

系统 SHALL 提供 `analytics/Dockerfile` 用于构建 Python FastAPI 服务的 Docker 镜像。

**构建策略**: 多阶段构建

**基础镜像**: `python:3.11-slim`

**构建步骤**:
1. 设置工作目录 `/app`
2. 复制 `requirements.txt`，执行 `pip install --no-cache-dir -r requirements.txt`
3. 复制 `src/` 目录
4. 创建 `data/` 目录（SQLite 数据将挂载到此处）
5. 暴露端口 8000
6. 启动命令: `uvicorn src.main:app --host 0.0.0.0 --port 8000`

**环境变量**: 不在 Dockerfile 中硬编码任何敏感信息，所有配置通过 `docker-compose.yml` 或 `.env` 注入

**健康检查**:
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/api/health')"
```

**镜像大小优化**:
- 使用 `slim` 基础镜像
- `--no-cache-dir` 避免 pip 缓存
- 不安装开发依赖

#### Scenario: Docker 构建成功

- **WHEN** 在 `analytics/` 目录执行 `docker build -t wh-op-analytics .`
- **THEN** 构建成功，镜像可运行，`docker run -p 8000:8000 wh-op-analytics` 后服务在 8000 端口可访问

#### Scenario: 健康检查通过

- **WHEN** 容器启动 30 秒后
- **THEN** Docker 健康检查执行 `GET /api/health`，返回 HTTP 200，容器状态为 healthy
