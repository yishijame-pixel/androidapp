# 企业级五子棋好友对战实现计划

## 概述

基于现有的五子棋框架，升级为完整的企业级竞技对战系统。现有架构已包含：
- 基础规则引擎 (`GomokuRules`)
- 房间管理与实时同步 (`PocketBaseRealtimeClient`, `GameRoomSyncCoordinator`)
- 落子逻辑 (`GameMoveRepository`)
- UI 组件 (`GomokuBoard`, `GomokuPlayerBar`)

## Phase 1: 规则引擎增强

### 1.1 禁手规则引擎
- **文件**: `social/game/engine/GomokuForbiddenRules.kt` (新建)
- **功能**: 
  - 三三禁手检测 (黑棋同时形成两个活三)
  - 四四禁手检测 (黑棋同时形成两个四)
  - 长连禁手检测 (六连以上)
- **实现**: 纯函数设计，方向扫描算法，支持单元测试

### 1.2 GomokuRules 扩展
- 新增 `checkForbidden(board, x, y, color)` 方法
- 新增 `isForbiddenMove()` 落子前校验
- 新增 `findWinningMoves()` 找出获胜点
- 新增 `findThreats()` 威胁检测（可选，为后期 AI 预留）

## Phase 2: 计时器系统

### 2.1 计时模型
- **文件**: `social/game/model/GomokuTimerModels.kt` (新建)
- **数据结构**:
```kotlin
data class GomokuTimerState(
    val blackRemainingMs: Long,    // 黑方剩余时间
    val whiteRemainingMs: Long,    // 白方剩余时间
    val lastTickMs: Long,          // 上次计时时间戳
    val currentTurnStartMs: Long,  // 当前回合开始时间
    val turnTimeLimitMs: Long,     // 单步限时（可选）
    val enabled: Boolean = true,
)
```

### 2.2 计时器逻辑
- **文件**: `social/game/engine/GomokuTimer.kt` (新建)
- 服务端主导计时，客户端 UI 显示
- 超时自动判负逻辑
- 存储到 `GomokuPlayState.timer` 字段

### 2.3 UI 集成
- 修改 `GomokuPlayerBar.kt` 添加倒计时显示
- 低于 30 秒闪烁警告
- 低于 10 秒音效提醒（可选）

## Phase 3: ELO 段位系统

### 3.1 数据库扩展
- **表**: `gomoku_player_stats` (PocketBase Collection)
```
pb_id: relation -> users
elo_rating: number (default 1200)
games_played: number
games_won: number
games_lost: number
games_drawn: number
win_streak: number
best_streak: number
updated_at: date
```

### 3.2 ELO 计算引擎
- **文件**: `social/game/engine/GomokuEloCalculator.kt` (新建)
- 标准 ELO 公式，K 因子 32（新手）/24（常规）/16（高段）
- 段位映射：青铜 < 1200, 白银 1200-1400, 黄金 1400-1600, 铂金 1600-1800, 钻石 > 1800

### 3.3 战绩更新
- 对局结束时自动更新双方 ELO
- 修改 `GameMoveRepository.submitGomokuMove()` 在胜负判定后调用

### 3.4 UI 展示
- 玩家卡片显示段位徽章
- 大厅显示双方 ELO 差距
- 趣玩中心个人战绩页

## Phase 4: WebSocket 实时同步优化

### 4.1 对局专用 Realtime 频道
- 修改 `PocketBaseRealtimeClient.kt`
- 新增 `subscribeGameMoves(roomId)` 方法
- 落子事件即时推送，替代轮询

### 4.2 GamePlaySyncManager (新建)
- **文件**: `social/game/GamePlaySyncManager.kt`
- 管理对局中的实时连接
- 断线重连逻辑
- 与 `GameRoomSyncCoordinator` 协作

### 4.3 ViewModel 优化
- 移除 `GamePlayViewModel` 中的 1.5s 轮询
- 改为 Realtime 事件驱动 + 长间隔兜底

## Phase 5: 观战系统

### 5.1 观战模式模型
- **文件**: 扩展 `GameRoomStatePayload`
- 新增 `spectators: List<String>` 字段
- 观战者权限：只读，不能落子

### 5.2 观战入口
- 大厅页添加「邀请观战」按钮
- 房间码支持观战模式加入
- 观战者列表 UI

### 5.3 实时同步
- 观战者自动订阅 `game_moves` 变更
- 低延迟棋盘同步

## Phase 6: 复盘系统

### 6.1 棋谱存储
- 对局结束时自动生成 SGF 格式棋谱
- 存储到 `game_rooms.sgf` 字段

### 6.2 复盘播放器
- **文件**: `ui/screens/socialgame/play/GomokuReplayScreen.kt` (新建)
- 逐步回放功能
- 分支变化探索（可选）

### 6.3 分享功能
- 棋谱导出为图片
- 分享到聊天/社交平台

## Phase 7: UI 增强

### 7.1 落子动画
- 修改 `GomokuBoard.kt`
- 新棋子淡入 + 缩放动画
- 最后一手高亮动画

### 7.2 游戏结束界面
- 胜负弹窗美化
- 显示双方 ELO 变化
- 复盘/再来一局按钮

### 7.3 音效系统
- 落子音效
- 倒计时警告音
- 胜负音效

## 实现顺序建议

1. **Week 1**: Phase 1 (禁手规则) + Phase 2 (计时器)
2. **Week 2**: Phase 4 (WebSocket 优化) + Phase 7.1 (落子动画)
3. **Week 3**: Phase 3 (ELO 段位) + Phase 7.2 (结束界面)
4. **Week 4**: Phase 5 (观战) + Phase 6 (复盘)

## 关键文件影响

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `GomokuRules.kt` | 修改 | 添加禁手检测 |
| `GomokuPlayState` | 修改 | 添加 timer 字段 |
| `GomokuBoard.kt` | 修改 | 动画、禁手标记 |
| `GomokuPlayerBar.kt` | 修改 | 计时器、段位徽章 |
| `GamePlayViewModel.kt` | 修改 | Realtime 驱动 |
| `PocketBaseRealtimeClient.kt` | 修改 | game_moves 订阅 |
| `GameMoveRepository.kt` | 修改 | ELO 更新 |
| `PocketBaseApiClient.kt` | 修改 | 新 API 方法 |

## 新增文件清单

```
social/game/engine/
├── GomokuForbiddenRules.kt   # 禁手规则引擎
├── GomokuTimer.kt            # 计时器逻辑
└── GomokuEloCalculator.kt    # ELO 计算

social/game/model/
├── GomokuTimerModels.kt      # 计时器数据模型
└── GomokuPlayerStats.kt      # 玩家战绩模型

social/game/
└── GamePlaySyncManager.kt    # 对局实时同步管理

ui/screens/socialgame/play/
├── GomokuReplayScreen.kt     # 复盘播放器
├── GomokuTimerDisplay.kt     # 计时器 UI
└── GomokuRankBadge.kt        # 段位徽章
```

## PocketBase Schema 变更

### 新增 Collection: `gomoku_player_stats`
```javascript
{
  "name": "gomoku_player_stats",
  "type": "base",
  "schema": [
    { "name": "user", "type": "relation", "required": true, "options": { "collectionId": "users" } },
    { "name": "elo_rating", "type": "number", "options": { "min": 0 } },
    { "name": "games_played", "type": "number" },
    { "name": "games_won", "type": "number" },
    { "name": "games_lost", "type": "number" },
    { "name": "games_drawn", "type": "number" },
    { "name": "win_streak", "type": "number" },
    { "name": "best_streak", "type": "number" }
  ]
}
```

### 修改 Collection: `game_rooms`
- 新增字段 `timer_state: json` 存储计时器状态
- 新增字段 `spectators: relation[]` 观战者列表
- 新增字段 `sgf: text` 棋谱存储

## 风险与注意事项

1. **计时器同步**: 服务端与客户端时间差可能导致显示不一致，需要 NTP 校准
2. **禁手判定复杂度**: 完整禁手规则计算量较大，需优化算法
3. **ELO 作弊防护**: 需防止通过故意输棋刷 ELO
4. **Realtime 连接稳定性**: 需处理断线重连、消息丢失场景

## 测试策略

1. **单元测试**: 禁手规则、ELO 计算
2. **集成测试**: 计时器同步、Realtime 推送
3. **E2E 测试**: 完整对局流程、观战模式
