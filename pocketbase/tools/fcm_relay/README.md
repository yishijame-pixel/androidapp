# FunLife FCM 中继

PocketBase Hook 在 `messages` / `friendships` 创建后 POST 到此服务，由 `firebase-admin` 调用 FCM HTTP v1。

## 1. 准备 Firebase 服务账号

1. [Firebase Console](https://console.firebase.google.com/) → 项目设置 → 服务账号
2. 「生成新的私钥」→ 保存 JSON（**勿提交 git**）
3. 环境变量 `FCM_SERVICE_ACCOUNT` 设为文件绝对路径，或整段 JSON 字符串

## 2. 本地启动

```powershell
cd pocketbase/tools/fcm_relay
npm install
$env:FCM_SERVICE_ACCOUNT = "D:\secrets\funlife-firebase-adminsdk.json"
$env:FCM_RELAY_KEY = "your-random-secret"   # 可选
npm start
```

健康检查：`GET http://127.0.0.1:8787/health`

## 3. 配置 PocketBase

```powershell
$env:FCM_RELAY_URL = "http://127.0.0.1:8787/push"
$env:FCM_RELAY_KEY = "your-random-secret"   # 与上面一致
cd pocketbase
.\pocketbase.exe serve --http=0.0.0.0:8090
```

## 4. 生产部署

- 将本服务部署到与 PocketBase 同 VPC / 内网（Cloudflare Tunnel 后的内网地址亦可）
- 务必设置 `FCM_RELAY_KEY`，仅 PocketBase 可访问 `/push`
- 可用 PM2、Docker、或 Cloudflare Worker（需自行封装 admin SDK）

## 请求格式

与 `pb_hooks/main.pb.js` 一致：

```json
{
  "token": "<device fcm token>",
  "title": "张三",
  "body": "你好",
  "data": {
    "type": "chat_message",
    "deep_link": "friend_chat/<senderPbId>",
    "message_id": "...",
    "conversation_id": "...",
    "peer_pb_id": "...",
    "body": "..."
  }
}
```
