# FunLife 企业级推送（FCM）接入指南

Phase 1 好友申请在 **App 进程存活** 时已通过 Realtime + 前台轮询实现秒级感知。  
**杀进程秒推** 需要 FCM + 本 Hook，按下列步骤接入。

## 架构

```
A 发好友申请
    │
    ▼
PocketBase friendships.create
    │
    ├── Realtime SSE ──► B App 存活 → 1~3s 收件箱
    │
    └── pb_hooks/main.pb.js
            │
            ▼
        FCM 中继服务 (FCM_RELAY_URL)
            │
            ▼
        Firebase → B 设备（进程已杀也能收到）
```

## 1. PocketBase Schema

运行 `setup-schema.ps1` 会自动添加 `users.fcm_token`（仅本人可写）。

或手动在 Admin → users 集合添加：

| 字段 | 类型 | 说明 |
|------|------|------|
| `fcm_token` | Text | Firebase 设备 Token，最长 512 |

## 2. Android 端（待接入 Firebase）

1. Firebase Console 创建项目，添加 Android App `com.example.funlife`
2. 下载 `google-services.json` 放到 `app/`
3. 在 `MessagingService.onNewToken` 中：

```kotlin
SocialPushTokenRegistry.saveToken(context, userId, token)
```

绑定成功后 `SocialSessionManager` 会自动上传到 PocketBase。

> 当前代码已预留 `SocialPushTokenRegistry`，未配置 Firebase 时不影响现有功能。

## 3. FCM 中继服务

PocketBase Hook 不直接调 Google API（需 Service Account），建议部署轻量中继：

**请求格式**（Hook 已按此发送）：

```json
POST FCM_RELAY_URL
Authorization: Bearer <FCM_RELAY_KEY>   // 可选

{
  "token": "<device fcm token>",
  "title": "新的好友申请",
  "body": "张三 请求添加你为好友",
  "data": {
    "type": "friend_request",
    "friendship_id": "xxx",
    "deep_link": "friends"
  }
}
```

可用 Cloudflare Worker / 云函数 / 自建 Node 服务，内部调用 [FCM HTTP v1](https://firebase.google.com/docs/cloud-messaging/send-message)。

## 4. 启动 PocketBase 并启用 Hook

Hook 文件位于 `pocketbase/pb_hooks/main.pb.js`，与 `pocketbase.exe` 同目录启动即可加载。

Windows 示例：

```powershell
$env:FCM_RELAY_URL = "https://your-relay.example.com/push"
$env:FCM_RELAY_KEY = "your-secret"
cd pocketbase
.\pocketbase.exe serve --http=0.0.0.0:8090
```

未设置 `FCM_RELAY_URL` 时 Hook 只记录日志，**不影响**好友增删改查。

## 5. 验收

| 场景 | 预期 |
|------|------|
| B 在前台 | Realtime → 1~3s 铃铛红点 |
| B 在后台（进程存活） | Realtime；若 SSE 断线则前台 60s 自适应补拉 |
| B 已杀进程 + FCM 已接 | 系统通知栏 + 点开进好友页 |
| 未接 FCM | 15 分钟 WorkManager 补拉 |

## 6. 安全建议

- `FCM_RELAY_KEY` 仅保存在服务端环境变量
- `fcm_token` 字段 updateRule 保持 `id = @request.auth.id`
- 生产环境 PocketBase 使用 HTTPS + 证书固定（App 已支持 `POCKETBASE_PIN`）
