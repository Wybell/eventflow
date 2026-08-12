# EventFlow 部署、更新与回滚手册

本手册适用于 Ubuntu 22.04、Docker Compose 与 Nginx。首次配置细节也可参考 [deploy/README.md](../deploy/README.md)。

## 运行拓扑与端口

| 组件 | 宿主机暴露 | 说明 |
| --- | --- | --- |
| frontend | `127.0.0.1:8083 -> 80` | 仅供宿主机 Nginx 转发。 |
| backend | 不直接暴露 | 由 frontend 容器反代访问。 |
| MySQL | `127.0.0.1:3307 -> 3306` | 不开放公网。 |
| Redis、RabbitMQ | 不暴露 | Docker 内部网络。 |
| 宿主机 Nginx | `80/443`，临时 `8084` | 公网入口。 |

安全组只应放行 SSH 管理 IP、HTTP 和 HTTPS。不要开放 `3306`、`3307`、`6379`、`5672`、`8080` 或 `8083`。

## 首次部署

```bash
git clone https://github.com/Wybell/eventflow.git /opt/eventflow
cd /opt/eventflow/deploy
cp .env.example .env
nano .env
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

必须替换 `.env` 内所有 `CHANGE_ME`。JWT 密钥可用 `openssl rand -base64 32` 生成。首次管理员通过 `EVENTFLOW_BOOTSTRAP_ADMIN_*` 临时初始化；确认能登录后，清空这些变量并重启 backend，避免密码留在服务器文件里。

首次启动时 Flyway 会执行迁移。生产表结构变更只新增 `backend/src/main/resources/db/migration/Vxxx__*.sql`，不要手工改库或修改已执行迁移。

## 宿主机 Nginx

域名配置示例：

```nginx
server {
    listen 80;
    server_name eventflow.example.com;
    client_max_body_size 6m;
    location / {
        proxy_pass http://127.0.0.1:8083;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

验证并重载：

```bash
nginx -t && systemctl reload nginx
```

域名备案前可使用 `deploy/nginx-eventflow-ip.conf`：

```bash
cp /opt/eventflow/deploy/nginx-eventflow-ip.conf /etc/nginx/conf.d/eventflow-temp.conf
nginx -t && systemctl reload nginx
```

此时通过 `http://服务器公网IP:8084` 访问。HTTPS 证书、私钥和 `.env` 都不应提交 Git。

## 日常发布

发布前先在本地确认项目代码已经提交和推送：

```bash
git status --short --branch
git push origin main
```

服务器只在要发布时操作，不需要持续盯着。所有 Compose 服务都使用 `restart: unless-stopped`，Docker 和服务器重启后会自动拉起。

### 只更新前端

适用于 React、TypeScript、CSS 和静态资源：

```bash
cd /opt/eventflow
git pull origin main
cd deploy
docker compose build frontend
docker compose up -d --no-deps frontend
docker compose ps frontend
```

使用 `build frontend` 后配合 `up -d --no-deps frontend`，范围只限前端，不会额外触发 backend 构建。

### 只更新后端

适用于 Java、Flyway 迁移和后端配置：

```bash
cd /opt/eventflow
git pull origin main
cd deploy
docker compose build backend
docker compose up -d --no-deps backend
docker compose logs -f backend
```

后端启动时关注 Flyway、数据库连接、JWT 必填配置和 `/actuator/health`。

### 同时更新前后端

```bash
cd /opt/eventflow
git pull origin main
cd deploy
docker compose up -d --build
docker compose ps
```

## 发布后验证

```bash
cd /opt/eventflow/deploy
docker compose ps
curl http://127.0.0.1:8083/actuator/health
```

`/actuator/health` 由 frontend Nginx 反代到 backend，预期 HTTP 200。浏览器用无痕窗口或测试账号走一遍：注册/登录、创建草稿、添加场次、审核、发布、报名、取消、头像上传和改密。前端发布后使用 `Ctrl+F5` 强制刷新。

## 回滚

先查看提交历史：

```bash
cd /opt/eventflow
git log --oneline -10
```

检出已知正常提交，并按改动范围重建：

```bash
git checkout <known-good-commit>
cd deploy
docker compose build frontend
docker compose up -d --no-deps frontend
```

如果回退包含后端和数据库变更，不能直接回退已执行 Flyway 迁移；应通过新的前向迁移修复生产数据结构。恢复到正常分支使用 `git checkout main` 和 `git pull origin main`。

## 日志、排障和备份

```bash
cd /opt/eventflow/deploy
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
docker compose logs --tail=200 mysql
docker compose ps
```

排障记录请求 URL、状态码、响应中的 `requestId`、发生时间和用户操作。常见排查方向：

- `502/504`：宿主机 Nginx、frontend 容器、`127.0.0.1:8083`。
- 页面刷新 `404`：frontend Nginx 的 `try_files $uri $uri/ /index.html`。
- 头像 `413`：宿主机 Nginx、frontend Nginx、Spring 5MB/6MB 限制。
- API `401`：access token、refresh token、JWT 密钥和服务器时间。
- backend 起不来：`.env`、MySQL healthcheck、Flyway 与 backend 日志。

数据库、Redis、RabbitMQ 和头像均使用 Docker volume。容器重建不会删除 volume，但仍要异机备份：

```bash
cd /opt/eventflow/deploy
chmod +x backup.sh
./backup.sh
```

建议用 cron 定时执行，并把备份复制到服务器外部。备份目录、证书和 `.env` 都不提交 Git。
