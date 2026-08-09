# 腾讯云部署

以下步骤按 Ubuntu 22.04 CVM 编写。公网只开放 SSH、HTTP 和 HTTPS，数据库、Redis、RabbitMQ 和后端都通过 Docker 内部网络通信。

## 1. 腾讯云准备

在 CVM 安全组中只放行：

- `22/tcp`：仅允许你的管理 IP
- `80/tcp`：HTTP
- `443/tcp`：HTTPS

不要放行 `3306`、`3307`、`6379`、`5672`、`15672` 或 `8080`。

安装 Docker 和 Compose 插件后，将项目上传到服务器，例如 `/opt/eventflow`。

## 2. 配置密钥

```bash
cd /opt/eventflow/deploy
cp .env.example .env
nano .env
```

必须修改所有 `CHANGE_ME` 字段。JWT 密钥可以使用下面命令生成：

```bash
openssl rand -base64 32
```

管理员初始化配置只在第一次启动前填写：

```env
EVENTFLOW_BOOTSTRAP_ADMIN_USERNAME=你的管理员账号
EVENTFLOW_BOOTSTRAP_ADMIN_PASSWORD=至少8位的管理员密码
EVENTFLOW_BOOTSTRAP_ADMIN_DISPLAY_NAME=管理员显示名
```

第一次成功启动并确认管理员可以登录后，清空这三个变量并重启，避免部署配置长期保留管理员密码。

## 3. 启动服务

```bash
cd /opt/eventflow/deploy
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

前端容器默认只监听服务器本机的 `127.0.0.1:8083`，不会占用服务器现有的 `80/443`。容器内 Nginx 会将 `/api` 转发给 Spring Boot，前端路由刷新会回退到 `index.html`。

在服务器现有 Nginx 中为 EventFlow 增加独立域名，并将请求转发到本机端口：

```nginx
server {
    listen 80;
    server_name eventflow.example.com;

    location / {
        proxy_pass http://127.0.0.1:8083;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

将 `eventflow.example.com` 替换为实际域名，执行 `nginx -t` 通过后再重载 Nginx。

## 4. 数据和头像

MySQL、Redis、RabbitMQ 使用 Docker volume 持久化，头像使用 `eventflow-avatar-data` 持久化。上线后仍需配置定时 MySQL 备份，并把备份复制到服务器之外的位置。

手动备份：

```bash
cd /opt/eventflow/deploy
chmod +x backup.sh
./backup.sh
```

备份默认写入 `deploy/backups`。可以使用 `EVENTFLOW_BACKUP_DIRECTORY` 指定其他目录，并通过 `cron` 定时执行。

## 5. 域名和 HTTPS

将域名 A 记录指向 CVM 公网 IP。可以使用腾讯云 SSL 证书或 Certbot，在 Nginx 前增加 HTTPS 终止。证书文件不要提交到 Git。

## 6. 上线检查

```bash
curl http://127.0.0.1:8083/actuator/health
docker compose ps
```

然后从浏览器验证注册、登录、管理员审核、发布、报名、取消报名和头像上传。确认头像和数据库在 `docker compose restart` 后仍然存在。
