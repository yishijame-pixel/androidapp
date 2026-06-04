# FunLife 微信级消息推送（FCM）接入指南

| 场景 | 通道 | 延迟 |
|------|------|------|
| App 前台 / 进程存活 | PocketBase Realtime SSE | 1~3 秒 |
| 后台 SSE 断线 | 前台自适应轮询 30s + 回前台立即补拉 | 30 秒内 |
| **进程已杀** | FCM 数据推送 + 系统通知 | 秒级（依赖 Google 与厂商通道） |

## 架构

```
A 发私聊 / 好友申请
        │
        ▼
PocketBase messages / friendships.create
        │
        ├── Realtime SSE ──► B App 存活 → SocialChatInbound → 通知
        │
        └── pb_hooks/main.pb.js
                │
                ▼
            FCM 中继 (tools/fcm_relay)
                │
                ▼
            Firebase → B 设备
                │
                ▼
            FunLifeFirebaseMessagingService
                │
                ├── FcmPushHandler → 入库 + ChatMessageNotifier
                └── ChatMessageExpeditedWorker 补拉会话
```

## 1. PocketBase Schema

运行 `setup-schema.ps1` 会自动添加 `users.fcm_token`（仅本人可写）。

## 2. Android 端

1. [Firebase Console](https://console.firebase.google.com/) 创建项目
2. 添加 Android 应用，包名 **`com.example.funlife`**
3. 下载 `google-services.json` → 复制到 **`app/google-services.json`**
4. 重新编译安装（Gradle 检测到文件后自动启用 `FCM_ENABLED=true`）

已实现：

- `FunLifeFirebaseMessagingService` — Token 刷新、数据消息分发
- `FcmPushBootstrap` — 冷启动 / 登录后上传 Token 到 PocketBase
- `FcmPushHandler` — 私聊 / 好友申请统一处理
- `ChatMessageExpeditedWorker` — FCM 唤醒后加急补拉会话

未放置 `google-services.json` 时：**不影响**现有 Realtime + 轮询，推送栈静默跳过 FCM。

### 通知权限

Android 13+ 需用户授予通知权限；App 启动流程中已有 POST_NOTIFICATIONS 请求。

## 3. FCM 中继服务

PocketBase Hook 不直接调 Google API，使用 `pocketbase/tools/fcm_relay/`：

```powershell
cd pocketbase/tools/fcm_relay
npm install
$env:FCM_SERVICE_ACCOUNT = "D:\path\to\firebase-adminsdk.json"
$env:FCM_RELAY_KEY = "random-secret"
npm start
```

详见 [tools/fcm_relay/README.md](tools/fcm_relay/README.md)。

## 4. 启动 PocketBase

Hook 位于 `pocketbase/pb_hooks/main.pb.js`，与 `pocketbase.exe` 同目录启动即可。

```powershell
$env:FCM_RELAY_URL = "http://127.0.0.1:8787/push"
$env:FCM_RELAY_KEY = "random-secret"
cd pocketbase
.\pocketbase.exe serve --http=0.0.0.0:8090
```

未设置 `FCM_RELAY_URL` 时 Hook 只记录日志，**不影响**聊天与好友功能。

## 5. 域名 / HTTPS（yishi.site）

| 项 | 值 |
|---|---|
| 根域名 | `yishi.site` |
| PocketBase | `https://pb.yishi.site` |
| 一键隧道 | `pocketbase/setup-tunnel-yishi.ps1` |

1. Cloudflare 接入 `yishi.site` → 运行 `setup-tunnel-yishi.ps1`
2. `local.properties`：`POCKETBASE_URL=https://pb.yishi.site`
3. FCM 中继仍跑本机 `127.0.0.1:8787`（PocketBase Hook 内网访问即可）
4. Release 可选：`POCKETBASE_PIN` 固定 Cloudflare 证书指纹

## 6. 验收清单

| 场景 | 预期 |
|------|------|
| B 在前台聊天页（与 A 对话） | 不弹系统通知，消息直接出现 |
| B 在前台其他页 | Realtime → 横幅 + 通知 |
| B 在后台（进程存活） | Realtime；断线则 30s 内补拉 |
| B 已杀进程 + FCM 已配置 | 系统通知 → 点开进入 `friend_chat/{peerPbId}` |
| A 发好友申请，B 杀进程 | 通知「新的好友申请」→ 好友页 |
| 未接 FCM | Realtime + 15 分钟好友 Worker 仍可用 |

## 7. 安全建议

- `FCM_RELAY_KEY` 仅保存在服务端环境变量
- Firebase 服务账号 JSON **勿提交 git**
- `fcm_token` 字段 updateRule：`id = @request.auth.id`
- 生产 PocketBase 使用 HTTPS + `POCKETBASE_PIN`
