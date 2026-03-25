# frontend-dockerfile

**所属服务**: frontend

## MODIFIED Requirements

### Requirement: Nginx 反向代理配置

系统 SHALL 在 `wh-op-platform/frontend/nginx.conf` 中配置：

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

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

nginx.conf SHALL 在 Docker Compose 默认网络中正确工作，`backend` 主机名通过 Docker 内部 DNS 解析。当前配置已满足此要求，无需修改。

#### Scenario: Vue Router 历史模式路由刷新

- **WHEN** 用户在 `/baseline` 页面刷新浏览器
- **THEN** Nginx 的 `try_files` 将请求回退到 `index.html`，Vue Router 接管路由，正确渲染 BaselineView

#### Scenario: API 请求代理到后端

- **WHEN** 前端发送 `GET /api/warehouses`
- **THEN** Nginx 将请求代理到 `http://backend:8080/api/warehouses`，返回后端响应

#### Scenario: Docker Compose 网络中服务发现

- **WHEN** 容器在 Docker Compose 默认网络中运行
- **THEN** `backend` 主机名通过 Docker 内部 DNS 解析到 Spring Boot 容器，Nginx 反向代理正常工作
