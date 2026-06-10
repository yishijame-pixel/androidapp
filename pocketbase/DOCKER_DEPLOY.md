# FunLife 社交服务 Docker 部署指南

本文档说明如何将 FunLife 社交后端（PocketBase + draw_ws + FCM 推送 + Cloudflare 隧道）部署到 Docker，替代 Windows 本机多窗口启动方式。

---

## 目录

1. [架构概览](#1-架构概览)
2. [前置要求](#2-前置要求)
3. [快速开始（本地 Docker）](#3-快速开始本地-docker)
4. [生产部署（Cloudflare 隧道）](#4-生产部署cloudflare-隧道)
5. [生产部署（Nginx + HTTPS）](#5-生产部署nginx--https)
6. [从 Windows 本机迁移数据](#6-从-windows-本机迁移数据)
7. [环境变量说明](#7-环境变量说明)
8. [日常运维](#8-日常运维)
9. [故障排查](#9-故障排查)
10. [与 start-public.ps1 对照](#10-与-start-publicps1-对照)

---

## 1. 架构概览

```
                    ┌─────────────────────────────────────┐
  Android App       │  Docker Host (Linux / Windows)      │
       │            │                                     │
       │  HTTPS     │  ┌─────────────┐                    │
       ├───────────►│  │ cloudflared │──► Cloudflare CDN  │
       │  WSS       │  └──────┬──────┘                    │
       │            │         │ funlife-net (bridge)        │
       │            │    ┌────┴────────────────────┐       │
       │            │    │ pocketbase      :8090   │       │
       │            │    │ draw-ws         :8790   │       │
       │            │    │ fcm-relay       :8787   │       │
       │            │    └─────────────────────────┘       │
       │            │  volumes: pb_data, secrets           │
       └────────────┴─────────────────────────────────────┘
```

| 服务 | 容器名 | 端口 | 是否对外暴露 | 说明 |
|------|--------|------|--------------|------|
| **PocketBase** | `funlife-pocketbase` | 8090 | 是（可映射到宿主机） | REST API、Realtime、Hooks、SQLite |
| **draw_ws** | `funlife-draw-ws` | 8790 | 是 | 你画我猜笔画 WebSocket 热路径 |
| **fcm_relay** | `funlife-fcm-relay` | 8787 | **否** | PocketBase Hook → Firebase 推送 |
| **cloudflared** | `funlife-cloudflared` | — | 出站隧道 | HTTPS/WSS 公网入口 |

### Compose Profiles

| Profile | 包含服务 | 使用场景 |
|---------|----------|----------|
| （默认） | pocketbase + draw-ws | 本地开发、内网测试 |
| `push` | + fcm-relay | 需要 FCM 离线推送 |
| `tunnel` | + cloudflared | 需要公网 HTTPS（pb.yishi.site） |

---

## 2. 前置要求

### 软件

| 工具 | 版本 | 用途 |
|------|------|------|
| Docker Engine | ≥ 24 | 容器运行时 |
| Docker Compose | v2（`docker compose`） | 编排 |
| Git | 任意 | 拉取代码 |

- **Windows**：安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)，启用 WSL2 后端。
- **Linux VPS**：安装 Docker Engine + compose 插件。

### 文件与密钥

| 路径 | 是否必需 | 说明 |
|------|----------|------|
| `pb_data/` | 首次可空 | SQLite 数据库与上传文件，**必须持久化** |
| `pb_hooks/` | 是（已在仓库） | PocketBase JS Hooks |
| `pb_migrations/` | 是（已在仓库） | 数据库 Schema 迁移 |
| `secrets/firebase-adminsdk.json` | 仅 push | Firebase 服务账号（`--profile push`） |
| `docker/cloudflared/config.yml` | 仅 tunnel | Cloudflare 隧道配置 |
| `docker/cloudflared/<UUID>.json` | 仅 tunnel | 隧道凭证（**勿提交 Git**） |

---

## 3. 快速开始（本地 Docker）

### 3.1 准备环境文件

```powershell
cd d:\soft\pocketbase
copy docker\.env.example .env
```

`.env` 中一般只需确认端口，推送相关可暂不填。

### 3.2 构建并启动（核心服务）

**Windows：**

```powershell
.\docker\up.ps1 -Build
```

**Linux / macOS：**

```bash
chmod +x docker/up.sh
./docker/up.sh --build
```

**或直接用 compose：**

```bash
docker compose build
docker compose up -d
```

### 3.3 验证

```powershell
# PocketBase
Invoke-RestMethod http://127.0.0.1:8090/api/health

# draw_ws
Invoke-RestMethod http://127.0.0.1:8790/health
```

管理后台：http://127.0.0.1:8090/_/

### 3.4 配置 Android 本地调试

`local.properties`：

```properties
POCKETBASE_URL=http://<你的电脑局域网IP>:8090
# DRAW_WS_URL 留空时，App 会按 POCKETBASE_URL 推导 ws 地址；
# 局域网调试建议显式指定：
DRAW_WS_URL=ws://<你的电脑局域网IP>:8790/draw-ws
```

> 真机访问宿主机时，Windows Docker Desktop 需放行防火墙；若容器端口已映射到 `0.0.0.0`，手机用电脑局域网 IP 即可。

---

## 4. 生产部署（Cloudflare 隧道）

与当前 `start-public.ps1` + `pb.yishi.site` 方案等价，但全部跑在 Docker 内。

### 4.1 创建隧道（一次性）

在**有 cloudflared 的机器**上执行（可与 Docker 宿主机相同）：

```bash
cloudflared tunnel login
cloudflared tunnel create funlife-pb
cloudflared tunnel route dns funlife-pb pb.yishi.site
```

记下输出的 **Tunnel UUID**。

### 4.2 配置隧道文件

```powershell
cd d:\soft\pocketbase\docker\cloudflared

# 1. 复制凭证（将 <UUID> 替换为实际值）
copy %USERPROFILE%\.cloudflared\<UUID>.json .\

# 2. 复制并编辑配置
copy config.example.yml config.yml
# 编辑 config.yml：替换 <TUNNEL_UUID>，确认 credentials-file 路径
```

`config.yml` 中服务地址已改为 Docker 内部 DNS：

- `http://pocketbase:8090`
- `http://draw-ws:8790`

### 4.3 配置 FCM 推送（可选）

```powershell
# 若尚未配置 Firebase
.\setup-push.ps1

# 编辑 .env
# FCM_RELAY_KEY=<与 secrets/fcm-relay.key 或 push.env 中一致>
# FCM_RELAY_URL=http://fcm-relay:8787/push
```

### 4.4 启动完整生产栈

```powershell
.\docker\up.ps1 -Build -Push -Tunnel
```

等价命令：

```bash
docker compose --profile push --profile tunnel up -d --build
```

### 4.5 公网验证

```powershell
Invoke-RestMethod https://pb.yishi.site/api/health
Invoke-RestMethod https://pb.yishi.site/draw-ws/health
```

Android 生产配置：

```properties
POCKETBASE_URL=https://pb.yishi.site
# DRAW_WS_URL 留空 → 自动 wss://pb.yishi.site/draw-ws
```

---

## 5. 生产部署（Nginx + HTTPS）

若使用自有 VPS + 域名证书（不用 Cloudflare 隧道），可：

1. 仅启动核心服务：`docker compose up -d`
2. 在宿主机安装 Nginx，参考 `docker/nginx/funlife.conf.example`
3. 用 certbot 申请证书
4. Nginx 反代到 `127.0.0.1:8090` 与 `127.0.0.1:8790`

关键：WebSocket 路径 `/draw-ws/` 必须配置 `Upgrade` 头与长超时（示例配置已包含）。

---

## 6. 从 Windows 本机迁移数据

若你已在 Windows 上用 `start-public.ps1` 跑过一段时间，数据库在 `pocketbase\pb_data\`：

1. **停止本机服务**（避免 SQLite 锁）：
   ```powershell
   taskkill /IM pocketbase.exe /F
   ```

2. **确认 `pb_data` 目录完整**（含 `data.db`、上传文件等）

3. **启动 Docker**：
   ```powershell
   docker compose up -d
   ```

   Docker 会直接挂载 `./pb_data`，无需额外复制。

4. **Hooks 热更新**：`pb_hooks/` 以只读卷挂载，改完 `main.pb.js` 后重启 PocketBase 容器：
   ```bash
   docker compose restart pocketbase
   ```

---

## 7. 环境变量说明

完整模板见 `docker/.env.example`。

### PocketBase

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PB_VERSION` | `0.39.0` | 构建镜像时下载的 PocketBase 版本 |
| `PB_HOST_PORT` | `8090` | 宿主机映射端口 |
| `FCM_RELAY_URL` | 空 | 启用 push 时设为 `http://fcm-relay:8787/push` |
| `FCM_RELAY_KEY` | 空 | 与 fcm_relay 一致的 Bearer 密钥 |

### draw_ws

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DRAW_WS_HOST_PORT` | `8790` | 宿主机映射端口 |
| `PB_BASE_URL` | （compose 内固定） | 容器内 `http://pocketbase:8090` |
| `DRAW_WS_RELAY_KEY` | 空 | 可选 WS 层 Bearer 鉴权 |
| `RING_SIZE` | `400` | 断线重连环形缓冲大小 |
| `MAX_MSG_PER_SEC` | `240` | 每连接每秒消息上限 |

### fcm_relay（profile: push）

| 变量 | 说明 |
|------|------|
| `FCM_RELAY_KEY` | **必填**，Bearer 鉴权 |
| `FCM_SERVICE_ACCOUNT` | 容器内固定为 `/run/secrets/firebase-adminsdk.json` |

---

## 8. 日常运维

### 启动 / 停止

```powershell
# 启动（含推送 + 隧道）
.\docker\up.ps1 -Push -Tunnel

# 停止
.\docker\down.ps1

# 停止并删除卷（危险：会清 pb_data 命名卷；bind mount 的 ./pb_data 不受影响）
.\docker\down.ps1 -Volumes
```

### 查看日志

```bash
docker compose logs -f pocketbase
docker compose logs -f draw-ws
docker compose --profile push logs -f fcm-relay
docker compose --profile tunnel logs -f cloudflared
```

### 更新代码后重新部署

```bash
git pull
docker compose --profile push --profile tunnel up -d --build
```

仅 Hooks 变更时：

```bash
docker compose restart pocketbase
```

### 备份

定期备份整个 `pb_data/` 目录：

```bash
tar -czf pb_data-backup-$(date +%Y%m%d).tar.gz pb_data/
```

### Schema 初始化（新环境）

```powershell
$env:POCKETBASE_URL = "https://pb.yishi.site"   # 或 http://127.0.0.1:8090
.\setup-schema.ps1
```

---

## 9. 故障排查

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| `draw_ws` 一直重启 | PocketBase 未就绪 | `docker compose logs pocketbase`，等 health 通过 |
| `https://pb.yishi.site/draw-ws/health` 502 | draw_ws 未启动或隧道配置错误 | 检查 `docker/cloudflared/config.yml` 中 `draw-ws:8790` |
| WebSocket 频繁断线 | 隧道 keepalive 不足 | 使用示例 config 中的 `originRequest` 配置 |
| FCM 不推送 | 未启用 push profile 或密钥不一致 | `docker compose --profile push ps`，核对 `.env` 与 `secrets/` |
| `pb_data` 权限错误（Linux） | 目录属主与容器用户不匹配 | `sudo chown -R 1000:1000 pb_data` 或放宽权限 |
| 端口冲突 | 本机仍在跑 `start-public.ps1` | `taskkill /IM pocketbase.exe /F` 并停掉占用 8090/8790 的进程 |
| ARM 服务器构建失败 | Dockerfile 使用 amd64 二进制 | 将 `linux_amd64` 改为 `linux_arm64` 或增加 build-arg |

### 健康检查命令

```bash
docker compose ps
docker inspect --format='{{.State.Health.Status}}' funlife-pocketbase
docker inspect --format='{{.State.Health.Status}}' funlife-draw-ws
```

---

## 10. 与 start-public.ps1 对照

| start-public.ps1 | Docker 等价 |
|------------------|-------------|
| `pocketbase.exe serve :8090` | 服务 `pocketbase` |
| `node tools/draw_ws/server.js` | 服务 `draw-ws` |
| `node tools/fcm_relay/server.js` | 服务 `fcm-relay`（`--profile push`） |
| `cloudflared tunnel run funlife-pb` | 服务 `cloudflared`（`--profile tunnel`） |
| `watch-draw-ws.ps1` 看门狗 | `restart: unless-stopped` + healthcheck |
| `%USERPROFILE%\.cloudflared\config.yml` | `docker/cloudflared/config.yml` |
| `secrets/push.env` | `pocketbase/.env` |

**何时仍用本机脚本？**

- 仅本地开发、不想装 Docker → `.\start.ps1` 或 `.\start-public.ps1`
- 需要直接看 Windows 窗口日志 → 本机脚本更直观

**何时用 Docker？**

- 部署到 Linux VPS / 云服务器
- 需要一键启停、自动重启、环境隔离
- 与 CI/CD 或编排平台集成

---

## 文件清单

```
pocketbase/
├── docker-compose.yml          # 主编排文件
├── .env                        # 本地环境变量（从 docker/.env.example 复制）
├── DOCKER_DEPLOY.md            # 本文档
└── docker/
    ├── Dockerfile.pocketbase
    ├── Dockerfile.draw-ws
    ├── Dockerfile.fcm-relay
    ├── .env.example
    ├── up.ps1 / up.sh          # 一键启动
    ├── down.ps1                # 一键停止
    ├── cloudflared/
    │   ├── config.example.yml
    │   ├── config.yml          # 实际配置（本地，勿提交）
    │   └── <UUID>.json         # 隧道凭证（本地，勿提交）
    └── nginx/
        └── funlife.conf.example
```

---

## 常见问题

**Q：可以不暴露 8090/8790 到公网吗？**  
A：可以。生产环境只开 cloudflared 出站隧道，宿主机防火墙不开放 8090/8790 即可。Compose 中的 `ports` 仅供本机调试，可在生产 `.env` 改为 `127.0.0.1:8090:8090` 或删除 `ports` 段。

**Q：draw_ws 能单独扩缩容吗？**  
A：当前版本房间状态在内存中，**不支持**多实例。水平扩展需要 Redis pub/sub（见 `tools/draw_ws/README.md` 中的规划）。

**Q：PocketBase 管理后台密码忘了？**  
A：`docker compose exec pocketbase pocketbase superuser upsert email@example.com newpassword`
