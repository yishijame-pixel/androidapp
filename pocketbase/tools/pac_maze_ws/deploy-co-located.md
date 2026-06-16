# Pac-Maze 权威 WebSocket 与 PocketBase 同区域部署

将 Pac-Maze 对战 WebSocket 与 PocketBase 部署在同一 VPS / 同一域名，经 Cloudflare 隧道或 Nginx 反代，可将状态同步 RTT 从数百 ms 降到 ~50–100ms。

## 拓扑

```
Android
  ├─ HTTPS  https://pb.example.com/api/...          → PocketBase :8090
  └─ WSS     wss://pb.example.com/pac-maze-ws/...   → pac-maze-ws :8791
```

## Nginx

将 [`nginx-pac-maze-ws.conf`](nginx-pac-maze-ws.conf) 片段并入站点配置，然后：

```bash
nginx -t && systemctl reload nginx
```

健康检查：

```http
GET https://pb.example.com/pac-maze-ws/health
```

期望响应：

```json
{"ok":true,"service":"funlife-pac-maze-ws","version":2,"pathPrefix":"/pac-maze-ws","rooms":0}
```

## Docker

```bash
# 在项目根目录
docker build -f pac-maze-server/Dockerfile -t funlife/pac-maze-ws .

docker run -d --name pac-maze-ws \
  -e PB_BASE_URL=http://host.docker.internal:8090 \
  -p 8791:8791 \
  funlife/pac-maze-ws
```

或与 PocketBase compose 叠加：

```bash
docker compose -f pocketbase/docker-compose.yml \
  -f pocketbase/tools/pac_maze_ws/docker-compose.override.yml up -d pac-maze-ws
```

## Android 配置

**同域部署（推荐）**：留空 `PAC_MAZE_WS_URL`，仅设置：

```properties
POCKETBASE_URL=https://pb.example.com
```

App 自动推导 `wss://pb.example.com/pac-maze-ws`。

**本地开发**：

```properties
POCKETBASE_URL=http://127.0.0.1:8090
PAC_MAZE_WS_URL=ws://10.0.2.2:8791
```

模拟器用 `10.0.2.2`；真机用电脑局域网 IP。

## 开发脚本

```powershell
# Terminal 1: PocketBase
cd pocketbase; .\start.ps1

# Terminal 2: 权威服
cd pac-maze-server; .\run-dev.ps1
```

## 验证

```powershell
cd pocketbase\tools\pac_maze_ws
.\deploy-co-located.ps1 -PbBase http://127.0.0.1:8090
```

## 环境变量

| 变量 | 默认 | 说明 |
|------|------|------|
| `PB_BASE_URL` | `http://127.0.0.1:8090` | PocketBase 地址 |
| `PAC_MAZE_WS_PORT` | `8791` | 监听端口 |
| `PAC_MAZE_DISCONNECT_MS` | `30000` | 断线判负宽限 |
| `PAC_MAZE_ROOM_GO_MS` | `8000` | ready 超时强制开局 |
