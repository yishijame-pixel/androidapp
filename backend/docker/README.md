# FunLife VIP 管理后台 · Docker

将 `backend/admin` 打包为容器，连接 CloudBase 数据库，供内网或 VPN 使用。**不要直接暴露到公网**；若需远程访问请走 HTTPS 反代并限制 IP。

## 快速启动（仅管理后台）

```powershell
cd backend
copy tools\.env.example tools\.env
# 编辑 tools\.env：TCB_*、ADMIN_PASSWORD_HASH、ADMIN_SESSION_SECRET

docker compose up -d --build
```

浏览器打开：`http://localhost:3300`（端口由 `ADMIN_HOST_PORT` 控制）。

## 生成管理员凭据

```powershell
cd backend\admin
node hash-password.js 你的强密码
```

把输出的 `ADMIN_PASSWORD_HASH`、`ADMIN_SESSION_SECRET` 写入 `backend/tools/.env`。

## 与 PocketBase 栈一起启动

在 `pocketbase` 目录使用 `admin` profile（变量写在 `pocketbase/.env`，参考 `pocketbase/docker/.env.example`）：

```powershell
cd pocketbase
docker compose --profile admin up -d --build
```

管理后台默认映射 `3300` 端口。若与 `backend/docker-compose.yml` 单独启动冲突（同名容器 `funlife-vip-admin`），请勿同时运行两套。

## 环境变量

通过 `backend/tools/.env` 注入（Compose 的 `env_file` + 挂载到容器内 `/app/tools/.env`）。

| 变量 | 必填 | 说明 |
|------|------|------|
| `TCB_ENV_ID` | 是 | CloudBase 环境 ID |
| `TCB_SECRET_ID` | 是 | 腾讯云 API 密钥 |
| `TCB_SECRET_KEY` | 是 | 腾讯云 API 密钥 |
| `ADMIN_PASSWORD_HASH` | 是 | bcrypt 哈希 |
| `ADMIN_SESSION_SECRET` | 是 | Cookie 签名密钥（≥16 字符） |
| `ADMIN_USERNAME` | 否 | 默认 `admin` |
| `ADMIN_COOKIE_SECURE` | 否 | HTTPS 反代后设为 `1` |
| `ADMIN_HOST_PORT` | 否 | 宿主机端口，默认 `3300` |

> 勿在 `docker-compose.yml` 的 `environment` 里写 `TCB_*: ${TCB_*:-}`，空值会覆盖 `env_file` 中的配置。

## 常用命令

```powershell
docker compose logs -f vip-admin
docker compose restart vip-admin
docker compose down
```

## 健康检查

`GET /login.html` — 未登录可访问，用于容器 `HEALTHCHECK`。
