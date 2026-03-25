# frontend-dockerfile

**所属服务**: frontend

## ADDED Requirements

### Requirement: 多阶段 Dockerfile

系统 SHALL 在 `wh-op-platform/frontend/Dockerfile` 中定义两阶段构建：

**阶段 1 — 构建（node:20-alpine）**:
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build
```

**阶段 2 — 运行（nginx:alpine）**:
```dockerfile
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Dockerfile MUST 使用 `.dockerignore` 排除 `node_modules`、`.git`、`dist` 目录。

#### Scenario: Docker 镜像构建成功

- **WHEN** 在 `frontend/` 目录执行 `docker build -t wh-op-frontend .`
- **THEN** 构建成功，最终镜像基于 nginx:alpine，包含 dist/ 静态文件和 nginx.conf

#### Scenario: 镜像体积合理

- **WHEN** 镜像构建完成
- **THEN** 最终镜像大小不超过 50MB（nginx:alpine 基础约 25MB + 静态文件）

### Requirement: Nginx 反向代理配置

系统 SHALL 在 `wh-op-platform/frontend/nginx.conf` 中配置：

```nginx
server {
    listen 80;
    server_name localhost;

    # 静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理 → Spring Boot 后端
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**反向代理规则**:
- `/api/` → `http://backend:8080`（所有 API 请求转发到 Spring Boot 后端，后端内部调用 Python 分析服务）
- `/` → 静态文件 `try_files`（Vue Router 历史模式兜底到 `index.html`）

#### Scenario: Vue Router 历史模式路由刷新

- **WHEN** 用户在 `/baseline` 页面刷新浏览器
- **THEN** Nginx 的 `try_files` 将请求回退到 `index.html`，Vue Router 接管路由，正确渲染 BaselineView

#### Scenario: API 请求代理到后端

- **WHEN** 前端发送 `GET /api/warehouses`
- **THEN** Nginx 将请求代理到 `http://backend:8080/api/warehouses`，返回后端响应

#### Scenario: Docker Compose 服务发现

- **WHEN** 容器在 Docker Compose 网络中运行
- **THEN** `backend` 主机名通过 Docker 内部 DNS 解析到 Spring Boot 容器

### Requirement: .dockerignore 配置

系统 SHALL 创建 `wh-op-platform/frontend/.dockerignore`，排除以下目录和文件：

```
node_modules
dist
.git
.gitignore
*.md
.env*
```

#### Scenario: 构建上下文排除 node_modules

- **WHEN** Docker 构建时发送构建上下文
- **THEN** `node_modules` 目录不包含在上下文中，减少构建上下文大小和传输时间
