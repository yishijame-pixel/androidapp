# FunLife 你画我猜 — 笔画 WebSocket 服务

## 架构（混合方案）

```
Android 画手                     Android 猜词方
    │                                  │
    │  stroke_chunk (16ms, binary)      │
    ▼                                  ▼
┌─────────────────────────────────────────────┐
│  draw_ws (WebSocket :8790)                  │
│  PB JWT 鉴权 · 房间广播 · 断线环形缓冲回放    │
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
| **WebSocket** | 实时笔画 `stroke_chunk`（二进制 v1） | ~60次/秒 |
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

同区域反代：`wss://pb.yishi.site/draw-ws/ws?token=...&room=...`

## 协议

### 控制面（JSON 文本）

`joined` / `replay` / `error` / `pong` 仍为 JSON。

### 数据面（二进制 v1，推荐）

| 字节 | 含义 |
|------|------|
| 0-1 | Magic `0xFD 0x47` |
| 2 | Version `1` |
| 3 | Type: `1` chunk · `2` end · `3` clear · `4` ping |
| … | strokeId · chunk · round · width · [seq] · flat float32 点阵 |
| 尾 | （服务端中继）from 用户 id |

JSON 仍兼容；Android 默认 `useBinaryWire()`。

## Android 配置

```properties
POCKETBASE_URL=https://pb.yishi.site
# 同区域：留空 DRAW_WS_URL → 自动 wss://pb.yishi.site/draw-ws
DRAW_WS_URL=
# 独立隧道：
# DRAW_WS_URL=wss://draw.yishi.site/ws
```

## 公网部署

### 推荐：与 PB 同 VPS / 同域名

见 [deploy-co-located.md](./deploy-co-located.md) 与 `deploy-co-located.ps1`。

### 备选：独立 Cloudflare 隧道

`draw.yishi.site` → `:8790`，`local.properties` 显式设置 `DRAW_WS_URL`。

## 客户端优化（已实现）

- [x] **Lobby 预连 WS**：`GameCenterViewModel.prewarmDrawWs`
- [x] **猜词方笔迹插值**：`DrawStrokeInterpolator`
- [x] **二进制热路径**：`DrawWsBinaryCodec`
- [x] **JWT 本地解码 + room 缓存**：`pbAuth.js`

## 企业级清单

- [x] PB JWT 鉴权 + 房间成员校验
- [x] 每连接 120 msg/s 限速
- [x] 房间环形缓冲（断线 replay）
- [x] 热路径无 DB 写入
- [x] 冷路径 PB 账本（Android stroke_end 归档）
- [x] 二进制 + JSON 双栈
- [ ] TLS 终止（Cloudflare / Nginx）
- [ ] 多实例 Redis pub/sub（用户量 >500 并发房）

## 画布同步验证

```powershell
# JVM 单测 + 真机抓 log 分析（猜词方连看画家快速连画 5~6 笔）
.\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1

# 仅分析已有 log
.\pocketbase\tools\draw_ws\test_draw_canvas_sync.ps1 -AnalyzeOnly -LogFile canvas.log

# 实时监听
.\pocketbase\tools\draw_ws\watch-draw-canvas-log.ps1
```

PASS 期望：`layer append` 链式增长，无 `double rebuild n=X within 150ms`。
