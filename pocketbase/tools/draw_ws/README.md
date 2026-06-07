# FunLife 你画我猜 — 笔画 WebSocket 服务

## 架构（混合方案）

```
Android 画手                     Android 猜词方
    │                                  │
    │  stroke_chunk (50ms)             │
    ▼                                  ▼
┌─────────────────────────────────────────────┐
│  draw_ws (WebSocket :8790)                  │
│  鉴权 PB JWT · 房间广播 · 断线环形缓冲回放    │
└─────────────────────────────────────────────┘
    │                                  │
    │  stroke_end / clear 归档          │  猜词 / 阶段 / 计分
    ▼                                  ▼
┌─────────────────────────────────────────────┐
│  PocketBase                                 │
│  game_rooms · game_moves 账本 · hooks       │
└─────────────────────────────────────────────┘
```

| 通道 | 用途 | 频率 |
|------|------|------|
| **WebSocket** | 实时笔画 `stroke_chunk` | ~20次/秒 |
| **PocketBase** | 房间、猜词、计分、`stroke_end` 归档 | 低频 |
| **PB Realtime (SSE)** | 聊天、邀请、阶段变更 | 低频 |

## 启动

```powershell
cd pocketbase\tools\draw_ws
npm install
$env:PB_BASE_URL="https://pb.yishi.site"
$env:PORT="8790"
npm start
```

健康检查：`GET http://127.0.0.1:8790/health`

## 连接

```
ws://127.0.0.1:8790/ws?token=<PB_JWT>&room=<roomId>
```

可选 Header：`Authorization: Bearer <DRAW_WS_RELAY_KEY>`

## 协议 v1

### 客户端 → 服务端

```json
{ "t": "stroke_chunk", "strokeId": "s1", "chunk": 0, "round": 1,
  "color": "#222222", "width": 4,
  "points": [[0.12, 0.34], [0.13, 0.35]] }

{ "t": "stroke_end", "strokeId": "s1", "round": 1,
  "color": "#222222", "width": 4,
  "points": [[...全部点...]] }

{ "t": "clear", "round": 1 }

{ "t": "ping" }
```

### 服务端 → 客户端

```json
{ "t": "joined", "userId": "...", "room": "...", "status": "playing" }
{ "t": "replay", "events": [ ...最近80条... ] }
{ "t": "stroke_chunk", "from": "pbUserId", "serverTs": 123, ... }
{ "t": "pong", "ts": 123 }
{ "t": "error", "code": "rate_limit" }
```

## 公网部署

与 FCM relay 相同模式：本机 8790 + Cloudflare 隧道 `draw.yishi.site`。

`local.properties`：

```properties
DRAW_WS_URL=wss://draw.yishi.site/ws
```

## 企业级清单

- [x] PB JWT 鉴权 + 房间成员校验
- [x] 每连接 40 msg/s 限速
- [x] 房间环形缓冲（断线 replay）
- [x] 热路径无 DB 写入
- [x] 冷路径 PB 账本（Android stroke_end 归档）
- [ ] TLS 终止（Cloudflare）
- [ ] 多实例 Redis pub/sub（用户量 >500 并发房）
