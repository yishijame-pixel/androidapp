# FunLife PocketBase — Phase 1 好友系统

## 1. 启动服务（开发）

```powershell
cd pocketbase
.\start.ps1        # PowerShell
# 或
start.bat          # CMD
```

- API：`http://127.0.0.1:8090/api/`
- 管理后台：`http://127.0.0.1:8090/_/`

### 以后重启

1. 结束旧进程（关闭终端，或任务管理器结束 `pocketbase.exe`）
2. 再运行 `start.ps1` / `start.bat`
3. **Schema 只需配置一次**；若删除 `pb_data/` 目录，需重新执行第 2 步初始化

### 真机连不上？（Windows 防火墙）

电脑能访问 `http://192.168.0.100:8090`，手机不行 → 多半是防火墙拦了 8090 入站：

```powershell
.\allow-firewall.ps1   # 或见脚本内 netsh 命令
```

**USB 调试备选**（不用 WiFi）：

```powershell
adb reverse tcp:8090 tcp:8090
```

`local.properties` 改为 `POCKETBASE_URL=http://127.0.0.1:8090` 后重新安装 App。

## 好友申请即时通知（企业级）

| 层级 | 机制 | 延迟 |
|------|------|------|
| L1 | **Realtime SSE**（进程存活） | 1~3 秒 |
| L2 | **事件驱动补拉**（回前台 / 断网恢复 / 点铃铛） | 即时 |
| L2b | **自适应兜底**（仅 SSE 不健康时，前台 60s） | 降级态 |
| L3 | **WorkManager** 15 分钟周期 | 后台兜底 |
| L4 | **FCM + pb_hooks**（杀进程） | 见 [PUSH_SETUP.md](./PUSH_SETUP.md) |

| 能力 | 说明 |
|------|------|
| **SocialSessionManager** | 登录即绑定，统一调度 Realtime / 同步 / 网络恢复 |
| **登录自动绑定** | 无需先进入好友页 |
| **铃铛红点** | 应用内收件箱；关闭系统通知也显示 |
| **杀进程秒推** | 配置 FCM 中继 + `users.fcm_token` |

双方均需 **登录过 App 一次**。接收方 App 在后台未被杀时，申请后应几乎立刻收到通知。

## 2. 自动初始化 Schema（推荐）

PocketBase 启动后，**另开终端**运行：

```powershell
cd pocketbase
.\setup-schema.ps1
```

脚本会自动完成：

| 集合 | 操作 |
|------|------|
| **users** | 新增 `funlife_local_id`(Number)、`funlife_username`(Text, Unique)、`online`(Bool) |
| **users Rules** | list/view 需登录；create 公开（App 注册）；update/delete 仅本人 |
| **friendships** | 新建 `requester`、`addressee`(Relation→users)、`status`(pending/accepted/blocked) |
| **friendships Rules** | 仅双方可见/改/删；create 仅 requester 本人 |

本地管理员与密码见 `DEV.local.md`（不进 git）。

### 手动配置（可选）

<details>
<summary>展开手动步骤</summary>

#### 扩展 `users`（Auth 集合）

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `funlife_local_id` | Number | 是 | 本地 Room userId |
| `funlife_username` | Text | 是 | FunLife 登录名，**Unique** |
| `online` | Bool | 否 | 在线状态 |

**users API Rules**

| 操作 | Rule |
|------|------|
| list/search | `@request.auth.id != ""` |
| view | `@request.auth.id != ""` |
| create | 留空（公开注册） |
| update | `id = @request.auth.id` |
| delete | `id = @request.auth.id` |

#### 新建 `friendships`

| 字段 | 类型 | 说明 |
|------|------|------|
| `requester` | Relation → users | 发起方 |
| `addressee` | Relation → users | 接收方 |
| `status` | Select | `pending` / `accepted` / `blocked` |

索引：`(requester, addressee)` Unique

**friendships API Rules**

| 操作 | Rule |
|------|------|
| list/view | `@request.auth.id = requester \|\| @request.auth.id = addressee` |
| create | `@request.auth.id = requester` |
| update | `@request.auth.id = requester \|\| @request.auth.id = addressee` |
| delete | `@request.auth.id = requester \|\| @request.auth.id = addressee` |

</details>

## 3. Android 配置

在 `local.properties`（不进 git）添加：

```properties
# 模拟器访问本机
POCKETBASE_URL=http://10.0.2.2:8090

# 真机同 WiFi 用电脑局域网 IP，例如：
# POCKETBASE_URL=http://192.168.0.100:8090
```

留空 `POCKETBASE_URL` → App 内「好友」提示未配置（不影响其他功能）。

修改后需重新编译 App。

## 4. 架构说明

```
FunLife 本地登录（Room）
        │
        ▼
SocialSessionManager（企业级会话中枢）
        ├── PocketBase Auth 绑定
        ├── FriendRealtimeHub（SSE + 指数退避）
        ├── SocialForegroundPoller（SSE 不健康时才 60s 补拉）
        ├── SocialNetworkMonitor（断网重连 → 事件补拉）
        ├── FriendRequestExpeditedWorker（加急补拉）
        └── SocialPushTokenRegistry → users.fcm_token（FCM 预留）
                │
                ▼
        friendships（云端）+ social_friend_cache（Room）
                │
                └── pb_hooks → FCM 中继（杀进程推送，见 PUSH_SETUP.md）
```

- **不迁移** Room 主数据 / VIP / AI
- **好友备注**仅存本地 Room，不上传云端
- **登出**自动清除 PB Token、本地链接与好友缓存

## 5. 验收清单（Phase 1）

- [ ] 两台设备/账号互加好友（搜索 `@username`）
- [ ] 接受 / 拒绝 pending 请求
- [ ] 删除好友
- [ ] 本地备注保存且换账号不可见
- [ ] 登出后重新登录可自动 re-link
- [ ] 未配置 POCKETBASE_URL 时显示友好提示

## 5b. Phase 2 私聊

| 集合 | 字段 | Rules 要点 |
|------|------|------------|
| **conversations** | `member_a`, `member_b`, `pair_key`(unique), `last_preview`, `last_message_at` | 仅双方可见/改 |
| **messages** | `conversation`, `sender`, `body` | 仅会话参与者可读；create 需 `sender = auth` |

App：`ChatInteractor` + Room v58（**按 userId 隔离**）；好友页 **好友/消息** Tab；Realtime 订阅 `messages`；前台横幅 + 系统通知。

| 能力 | 说明 |
|------|------|
| 会话列表 | 本地 Room + 云端 sync，按最后消息时间排序 |
| Realtime | SSE 同时订阅 `friendships` + `messages` |
| 消息通知 | 非当前聊天页：系统通知 + App 内 heads-up |
| ChatFocusTracker | 正在看的会话不重复弹通知 |
| 降级补拉 | SSE 不健康时 60s 同步会话/消息 |

### Phase 2 验收清单

- [ ] 两名已接受好友可互发文字消息
- [ ] **消息 Tab** 显示会话列表与预览
- [ ] Realtime：对方发消息几乎即时出现在聊天页
- [ ] 不在聊天页时收到通知，点击跳进对应私聊
- [ ] 消息本地 Room 缓存，换账号不可见
- [ ] 非参与者无法读取会话消息（PB Rules）
- [ ] 登出清除本地 chat 缓存

```powershell
.\setup-schema.ps1              # 含 conversations / messages
node pocketbase/tools/test_social_chat_e2e.js
```

## 6. 企业级自动化测试

### 一键跑社交全套（推荐）

先启动 PocketBase，再：

```powershell
.\scripts\run-social-tests.ps1
```

| 参数 | 说明 |
|------|------|
| `-BaseUrl http://192.168.x.x:8090` | 指定 PB 地址 |
| `-SetupSchema` | E2E 前自动执行 `setup-schema.ps1` |
| `-SkipE2E` | 仅跑 Kotlin 纯逻辑单元测试 |
| `-SkipKotlin` | 仅跑 Node E2E（需 PB 运行） |
| `-KeepData` | 保留 E2E 创建的测试账号 |
| `-CI` | CI 模式（无颜色） |

### E2E 覆盖场景（`pocketbase/tools/test_social_e2e.js`）

| 类别 | 场景 |
|------|------|
| 基础设施 | `/api/health` |
| 账号 | 双用户注册、Token refresh |
| 搜索 | 命中 / 未命中 |
| 好友申请 | 发送、重复拦截、pending+expand |
| 操作 | 拒绝、接受、删除 |
| 安全 | 未登录 401、代他人发申请 403 |
| 资料 | `getUserById` 补全（通知 fallback） |
| 清理 | Admin 删除测试数据 |

### 私聊 E2E（`pocketbase/tools/test_social_chat_e2e.js`）

| 类别 | 场景 |
|------|------|
| 好友 | A↔B 接受好友 |
| 会话 | findOrCreate + pair_key 幂等 |
| 消息 | 双向发送 + 列表拉取 |
| 安全 | 非参与者 C 无法读消息 |
| 清理 | Admin 删除测试数据 |

### 纳入全栈测试

```powershell
.\scripts\run-all-tests.ps1              # 含社交 E2E（需 PB 运行）
.\scripts\run-all-tests.ps1 -SkipSocial  # 无 PB 时跳过
```
