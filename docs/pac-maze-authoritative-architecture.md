# Pac-Maze 在线对战 — 权威游戏服架构（企业级）

> 版本：1.0 · 状态：Phase 1 实施中  
> 目标产品形态：《Pac-Man 256》式实时双人对战 / 大乱斗扩展

---

## 1. 背景与问题陈述

### 1.1 现状（已废弃/过渡）

| 方案 | 描述 | 致命缺陷 |
|------|------|----------|
| 双端 Lockstep | 两客户端各跑 `PacMazeOnlineSimulation` + PocketBase 传输入 | tick 不对齐 → 位置/吃豆分叉 |
| 房主权威 Phase1 | 房主模拟 + PB move 传快照 | 房主掉线即崩；HTTP 落库延迟高；不可扩展 AI/观战 |

### 1.2 目标

- **单一世界真相**：吃豆、撞鬼、得分只在一处判定
- **客户端**：Compose 渲染 + 摇杆输入 + 插值（不跑完整模拟）
- **大厅/档案**：PocketBase（账号、房间、ELO、战绩）
- **对局热路径**：Ktor WebSocket 权威服（60 tick/s 模拟，15–20Hz 状态广播）

---

## 2. 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        PocketBase (冷路径)                        │
│  users · game_rooms · game_moves(仅终局/回放) · ELO · 邀请推送    │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST：建房/准备/开始/结算
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              pac-maze-server (Ktor · JVM · 权威)                 │
│  · PB JWT 鉴权 + 房间成员校验                                      │
│  · RoomSession：60Hz tick · PacMazeOnlineSimulation              │
│  · WebSocket：input ↑  state ↓  joined/replay/room_go            │
└────────────┬───────────────────────────────┬────────────────────┘
             │ wss://host/pac-maze-ws         │
     ┌───────▼───────┐               ┌───────▼───────┐
     │ Android 客户端 A │               │ Android 客户端 B │
     │ 渲染 + 输入      │               │ 渲染 + 输入      │
     └───────────────┘               └───────────────┘
```

### 2.1 职责边界（SRP）

| 组件 | 负责 | 不负责 |
|------|------|--------|
| **PocketBase** | 身份、房间生命周期、匹配元数据、战绩持久化、Realtime 推送邀请 | 每 tick 位置、每帧输入 |
| **pac-maze-server** | 权威模拟、碰撞、吃豆、胜负、断线判定、AI（后期）、观战 fan-out（后期） | 用户注册、长期存储 |
| **pac-maze-engine** | 纯 Kotlin 游戏规则、确定性 RNG、快照 codec | UI、网络、IO |
| **Android app** | Compose 渲染、摇杆、WS 会话、插值、本地音效 | 在线吃豆判定 |

---

## 3. 模块结构（Gradle Monorepo）

```
FunLife/
├── app/                          # Android Compose 客户端
├── pac-maze-engine/              # JVM：模拟核心 + 协议 codec（与 app 共享源码）
├── pac-maze-server/              # Ktor 权威服
├── PageFlip/                     # 既有模块
└── docs/
    └── pac-maze-authoritative-architecture.md
```

### 3.1 `:pac-maze-engine`

- **类型**：`kotlin("jvm")` library
- **源码**：与 `app/.../pacmaze/*.kt` 共享（exclude Android-only：`PacMazeLevelSource.kt` 等）
- **补充**：`PacMazeArenaParser.kt`（JVM 地图解析）、`PacMazeWsProtocol.kt`（消息定义）
- **依赖**：Gson（与现有一致）
- **测试**：JUnit 快照 round-trip、模拟 determinism、吃豆单测

### 3.2 `:pac-maze-server`

- **类型**：Ktor 2.x + Netty
- **依赖**：`:pac-maze-engine`、Ktor WebSockets、Gson、OkHttp（PB 校验）
- **资源**：`resources/pac_maze/arenas/*.json`（从 app assets 同步）
- **入口**：`Application.kt` → `/pac-maze-ws`

### 3.3 `:app` 变更

- `implementation(project(":pac-maze-engine"))`
- 新增 `social/pacmazews/`：`PacMazeWsConfig`、`PacMazeWsSession`、`PacMazeWsProtocol`
- `PacMazeOnlineViewModel`：若 `PacMazeWsConfig.isEnabled()` → 仅 WS；否则 fallback 房主权威 PB（过渡）

---

## 4. WebSocket 协议

### 4.1 连接

```
wss://<host>/pac-maze-ws?roomId=<id>&token=<PB_JWT>
```

服务端流程：

1. 解码 JWT → `userId`（PB record id）
2. `GET /api/collections/game_rooms/records/:roomId` 校验 host/guest
3. `game_type == pac_maze` 且 `status == playing`（或 counting down）
4. 回复 `joined` → 若对局进行中附带最近一帧 `state` 快照

### 4.2 消息类型（JSON，`t` 字段 discriminant）

#### 客户端 → 服

| `t` | 字段 | 说明 |
|-----|------|------|
| `input` | `dir?`, `attack`, `seq` | 摇杆四向；无 dir=松手 |
| `ping` | `clientMs` |  RTT 测量 |
| `ready` | — | 倒计时齐步走（可选 Phase2） |

#### 服 → 客户端

| `t` | 字段 | 说明 |
|-----|------|------|
| `joined` | `entityId`, `isHost`, `tick`, `state?` | 握手完成 |
| `state` | `tick`, `phase`, `entities`, `tiles`, `scores`, … | 权威快照（见 §4.3） |
| `room_go` | `startMs` | 统一开局时刻（倒计时结束） |
| `match_end` | `winner`, `scores`, `reason` | 终局 |
| `pong` | `clientMs`, `serverMs` | |
| `error` | `code`, `message` | |

### 4.3 状态快照 schema（`state`）

与 `PacMazeStateSnapshot.encode()` 对齐：

```json
{
  "t": "state",
  "tick": 120,
  "phase": "PLAYING",
  "tiles": [0,1,1,...],
  "w": 13, "h": 15,
  "entities": [
    {"id":"pac_a","role":"pac_a","x":1.5,"y":7.2,"dir":"RIGHT","facing":"RIGHT","vx":6,"vy":0}
  ],
  "score_a": 10, "score_b": 7,
  "lives_a": 3, "lives_b": 3,
  "pellets": 180,
  "power": 0, "ghost_release": 200,
  "elapsed": 12,
  "zone_a": [1,2,3], "zone_b": [4,5,6]
}
```

**带宽优化（Phase 3）**：delta 压缩 tiles（仅变更 index）、protobuf/binary frame。

---

## 5. 服务端设计

### 5.1 进程模型

```
Application
  └── PacMazeRoomRegistry (ConcurrentHashMap<roomId, PacMazeRoomSession>)
        └── PacMazeRoomSession
              ├── peers: Map<userId, WebSocketSession>
              ├── world: PacMazeWorldState
              ├── inputs: Map<entityId, PacMazeTickInput>
              ├── tickJob: 60Hz coroutine
              └── config: PacMazeOnlineMatchConfig
```

### 5.2 Tick 循环（固定 60Hz）

```kotlin
while (session.active && world.phase == PLAYING) {
    val inputs = collectInputsForTick()  // 各玩家最新 input
    world = PacMazeOnlineSimulation.tick(world, inputs, level, config, attacks)
    if (world.tick % 4L == 0L) broadcastState()  // ~15Hz
    delay(16.ms)  // 漂移校正见 §5.3
}
```

### 5.3 时间模型

- **权威 tick**：仅服务端递增，客户端 **不得** 用本地墙钟推模拟
- **渲染**：客户端对 `state` 做 66ms 插值（`lerp` 实体 x/y）
- **可选预测（Phase 2）**：仅对 **本地玩家** 超前 1–2 帧渲染，收到 `state` 时 soft 校正

### 5.4 鉴权（复用 draw-ws 模式）

- 环境变量：`PB_BASE_URL`, `PAC_MAZE_WS_PORT`（默认 8791）
- `PacMazePbAuth`：JWT 解码 + room GET + `game_type=pac_maze`
- Token/Room 缓存 TTL：5min / 60s

### 5.5 断线与重连

| 事件 | 行为 |
|------|------|
| 客人断线 < 30s | 输入变 Inactive，游戏继续 |
| 客人断线 > 30s | 判负或暂停（可配置） |
| 房主断线 | **Phase 1**：会话终止；**Phase 3**：迁移到备机/托管服 |
| 重连 | 新 WS → `joined` + 最新 `state` 全量快照 |

### 5.6 与 PocketBase 协作

```
[Lobby] 双方 ready → PB patch game_rooms.status=playing, pac_maze.started_at_ms
[Play]  仅 WS 热路径
[End]   server → POST PB (server-side hook 或客户端代发) finishPacMatch
```

**PB `game_moves`**：在线对局 **不再** 写入每 tick 输入；仅可选保存：

- `pac_match_replay`：压缩输入序列 + seed（赛后）
- `pac_surrender`：冷路径

---

## 6. 客户端设计

### 6.1 PacMazeWsSession（对标 DrawWsSession）

- OkHttp WebSocket
- 连接：`PacMazeWsConfig.url()` ← `PAC_MAZE_WS_URL` 或 PB 同域 `/pac-maze-ws`
- 事件流：`Joined`, `State`, `MatchEnd`, `Disconnected`
- 断线指数退避重连；`joined` 前 outbound 队列

### 6.2 PacMazeOnlineViewModel（WS 模式）

```
onJoystick → session.send(input)
onState    → simulationWorld = decode(snapshot); publishRenderFrame
advanceFrame → 仅插值 blend，不调用 PacMazeOnlineSimulation.tick
```

### 6.3 配置

`local.properties`:

```properties
PAC_MAZE_WS_URL=ws://10.0.2.2:8791/pac-maze-ws
# 或 wss://your.domain/pac-maze-ws
```

`BuildConfig.PAC_MAZE_WS_URL`（与 `DRAW_WS_URL` 同级）

---

## 7. 部署拓扑

### 7.1 开发

```powershell
# Terminal 1: PocketBase
cd pocketbase && .\start.ps1

# Terminal 2: Pac-Maze WS Server
.\pac-maze-server\run-dev.ps1

# Android Emulator: PAC_MAZE_WS_URL=ws://10.0.2.2:8791/pac-maze-ws
```

### 7.2 生产（Docker Compose 扩展）

```yaml
services:
  pocketbase: ...
  pac-maze-ws:
    build: ./pac-maze-server
    environment:
      PB_BASE_URL: http://pocketbase:8090
      PORT: 8791
    ports: ["8791:8791"]
  nginx:
    # wss://domain/pac-maze-ws → pac-maze-ws:8791
```

参考：`pocketbase/tools/draw_ws/deploy-co-located.ps1`

---

## 8. 实施路线图

### Phase 1 — MVP 权威服（当前迭代）

- [x] 架构文档
- [x] `:pac-maze-engine` JVM 模块
- [x] `:pac-maze-server` Ktor WS + 60Hz tick + 15Hz state
- [x] Android `PacMazeWsSession` + ViewModel 接入
- [x] `local.properties` + 开发脚本
- [ ] 双端真机/模拟器对战验证

**验收标准**：双方屏幕角色位置一致；吃豆双方同步；摇杆推杆 200ms 内可见移动。

### Phase 2 — 体验与健壮性

- [x] 本地玩家 1–2 帧预测 + 软校正（`PacMazeOnlineRenderSync`）
- [x] 断线重连 + 30s 宽限（`PAC_MAZE_DISCONNECT_MS`）
- [x] `ready` / `room_go` 齐步走（双端 ready 或 8s 超时）
- [x] Docker + Nginx 同域部署文档与脚本
- [ ] 服务端 metrics（tick 耗时、p99 延迟、房间数）
- [ ] 集成测试：`tools/pac_maze_ws/test_sync_e2e.js`

### Phase 3 — 扩展能力

- [ ] AI 玩家（服务端 bot input 源）
- [ ] 观战（read-only state 订阅）
- [ ] 道具/技能（engine 已有基础，服内启用）
- [ ] Binary/protobuf 快照
- [ ] 独立水平扩展（Redis room routing / 单服多实例）

### Phase 4 — 运营

- [ ] 排行榜 / 赛季 / 反作弊（服务端校验输入合法性：方向突变、速度上限）
- [ ] 回放系统（输入 log + seed 重放）
- [ ] 移除 PB 热路径 fallback 与旧 lockstep 代码

---

## 9. 安全与反作弊

| 威胁 | 对策 |
|------|------|
| 伪造吃豆 | 客户端无判定权；state 仅服广播 |
| 伪造输入 | 服校验：每 tick 最多 1 方向；非法 dir 丢弃 |
| 未授权 WS | PB JWT + room 成员 |
| 洪水攻击 | 每连接 `MAX_INPUT_PER_SEC=60` 限流 |
| 重放攻击 | `seq` 单调递增；过期 session 拒绝 |

---

## 10. 测试策略

| 层级 | 内容 |
|------|------|
| 单元 | `PacMazeStateSnapshot` round-trip；`PacMazeOnlineSimulation` determinism |
| 集成 | Ktor testApplication：2 mock client，输入→state 一致 |
| E2E | Node 脚本模拟双客户端 WS；Android 双开 |
|  soak | 10min 双人对战无 desync；内存稳定 |

---

## 11. 附录：与 draw-ws 对照

| | draw-ws (Node) | pac-maze-ws (Ktor) |
|--|----------------|---------------------|
| 热路径 | 笔画中继广播 | **权威模拟** + state 广播 |
| 状态 | 客户端自绘 | 服唯一 world |
| 语言 | JavaScript | Kotlin（与 app 共享 engine） |
| 端口 | 8790 | 8791 |
| PB 职责 | 冷路径猜词 | 冷路径房间/战绩 |

---

## 12. 变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-06-14 | 1.0 | 初版：Ktor 权威服方案，弃用双端 lockstep |
