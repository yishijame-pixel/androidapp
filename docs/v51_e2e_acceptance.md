# v51 云代理 真机端到端验收清单

> 全部 6 步通过 = AI KEY 上云改造在真机上验证通过 ✅

## 准备

```powershell
# 终端 1：实时看客户端 AI 日志
adb logcat -c
adb logcat -s LetterDeliveryWorker AiService LetterRepo
```

```powershell
# 终端 2：查云端配额表（每次操作后跑一次看记录）
node d:\soft\backend\tools\_debug_quota.js
```

## Step 1 · 时光信箱代理（VIP 必需）

> 普通用户每月只能写 1 封，要确保你已用卡密激活了 VIP（任意等级）

1. App → 时光信箱 → 写信给 "5 年前的我"
2. **deliveryAt 选"立即"**
3. 几秒后收到回信通知

**期望日志**（终端 1）：
```
LetterDeliveryWorker: scan due outgoing letters: 1
AiService:            cloud chat ok (... 实际是 LetterCloudRepository 路径)
LetterDeliveryWorker: cloud reply ok used=1/<你的 vipLevel 配额>
LetterDeliveryWorker: reply generated for letter <id> (failure=null)
```

**期望服务端**（终端 2）：`letter_quota` 表新增一条 `deviceId=<你设备指纹>, ym=202605, count=1, lastLetterId=<letterId>`

✅ 通过 = 信能收到 + 服务端有记录

---

## Step 2 · 配额耗尽降级（用普通账号）

1. **退出 VIP / 用没激活过的设备**
2. 写第一封信 → 应正常收到（普通用户 quota = 1）
3. 写第二封信 → 收到一封温柔的"本月信件太多"兜底回信

**期望日志**：
```
LetterDeliveryWorker: cloud quota exceeded 1/1
LetterDeliveryWorker: reply generated for letter <id> (failure=quota_exceeded)
```

✅ 通过 = 第二封确实是兜底文案，**不是真的 AI 回信**（防破解者绕过云配额）

---

## Step 3 · 聊天记账代理

1. App → 聊天记账页
2. 输入一笔账，比如"早餐 12 元"
3. AI 应回复一两句话（不超 30 字）

**期望日志**：
```
AiService: cloud chat ok used=1/20
```

**期望服务端**：`chat_ai_quota` 表新增 `deviceId=<你>, ymd=20260527, count=1`

---

## Step 4 · 升级按钮鲜艳验证

1. 触发任意 QuotaBanner（Step 2 中的"额度已满"会触发）
2. 看到的按钮应是 **橙→粉→金渐变 + ✨ 立即升级解锁** 白色加粗文字
3. **不应是**之前的单色品牌色

✅ 通过 = 视觉上明显比"稍后再说"按钮鲜艳

---

## Step 5 · 凭证拒绝验证（高级）

> 模拟破解者改本地凭证

1. 用 ADB / Frida 把本地 EncryptedSharedPreferences 里的 cert.exp 改成过期值
2. 重启 App，写信
3. 应该被云端拒绝（`CERT_EXPIRED`），客户端不降级直连

**期望**：
```
LetterDeliveryWorker: cloud rejected: CERT_EXPIRED
LetterDeliveryWorker: reply generated (failure=cloud_rejected_CERT_EXPIRED)
```

> 跳过此步也可以——这只是验证防破解保护

---

## Step 6 · 飞行模式断网降级

1. 打开飞行模式
2. 写信
3. **会进 PENDING 等下一轮 Worker**，开网后才生成回信

**期望**：网恢复后日志：
```
LetterDeliveryWorker: cloud recoverable, fallback to direct: NETWORK_ERROR
```
然后客户端直连 AI（如果还有 AI_API_KEY 配置）

> 如果你已经做了 A · 删除 AI_API_KEY，这一步会走 fallbackReply 兜底文案。

---

## 全绿 = v51 上线就绪

完成以上 6 步后：
- [ ] Step 1 时光信箱代理调用成功
- [ ] Step 2 普通用户配额耗尽走兜底
- [ ] Step 3 聊天记账代理调用成功
- [ ] Step 4 升级按钮鲜艳生效
- [ ] Step 5 凭证拒绝（可选）
- [ ] Step 6 断网降级（可选）

任何一步失败 → 把 logcat 输出贴出来，我定位问题。
