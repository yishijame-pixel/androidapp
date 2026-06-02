# FunLife 测试报告

**日期**：2026-05-26  
**执行人**：Cascade（自动化部分） + 用户（真机部分）  
**应用版本**：debug build  
**后端环境**：`funlife-prod-d8gxf7og0518b8253`

---

## 1. 摘要

| 维度 | 通过 | 失败 | 待人工 |
|---|---|---|---|
| 后端安全主线（红队）| 16 | 0 | 0 |
| 客户端编译 / lint | 2 | 0 | 0 |
| 客户端 UI 流程 | 0 | 0 | 25+ |
| **新发现并修复的生产漏洞** | — | — | **3** |

**结论**：**后端核心防御已全部生效**；客户端 UI 必须由你本地真机/模拟器走一遍。

---

## 2. 生产漏洞与修复

### 2.1 漏洞 P1：`HMAC_SECRET` 未注入到 `register_log` / `coin_log`

| 项 | 内容 |
|---|---|
| 严重度 | 🔴 高 |
| 现象 | `register_log` 抛 `IDENTITY_SECRET 未配置` → 返回 `DB_ERROR` → 客户端拿不到 `deviceToken` → 所有 `coin_log` 调用因无 token 被 `AUTH_REQUIRED` 拦截 |
| 实际影响 | 金币流水**从未上传**云端；后台监测/异常告警/反作弊**全部失效** |
| 修复 | `@/d:/soft/backend/cloudbaserc.json` 给 `register_log`、`coin_log` 加 `envVariables.HMAC_SECRET` |
| 验证 | `_redteam_run2.ps1` A 组 7 用例全部通过 |
| 状态 | ✅ 已修复并部署 |

### 2.2 漏洞 P2：3 个数据集合从未创建

| 集合 | 用途 | 影响 |
|---|---|---|
| `vip_revocations` | VIP 凭证吊销名单 | 即使后台想吊销某用户也无效 |
| `vip_coin_snapshots` | 金币余额快照（反作弊） | 异常检测无样本 |
| `vip_coin_nonces` | nonce 去重 | 重放攻击未被拦截 |

| 项 | 内容 |
|---|---|
| 严重度 | 🔴 高 |
| 修复 | 通过 `db.createCollection()` 创建 3 个集合 |
| 验证 | redteam 函数 `inspect_rev` 显示集合可读写 |
| 状态 | ✅ 已修复 |

### 2.3 漏洞 P3：`verify` 吊销查询用脆弱字符串 key

| 项 | 内容 |
|---|---|
| 严重度 | 🟡 中 |
| 现象 | 原代码 `REVOCATIONS.doc("d_" + deviceId).get()` 依赖人工拼 key 规则，运维与查询双方易不一致 |
| 修复 | 改为 `where({ deviceId, skuCode }).get()` 语义查询 |
| 位置 | `@/d:/soft/backend/functions/verify/index.js:122-139` |
| 状态 | ✅ 已修复并部署 |

---

## 3. 客户端代码改动汇总

| # | 改动 | 文件 | 用途 |
|---|---|---|---|
| 1 | 多用户计分隔离 | `@/d:/soft/app/src/main/java/com/example/funlife/data/database/AppDatabase.kt`、`ScoreViewModel.kt` | 防止 A 账号看到 B 账号积分 |
| 2 | OperationLog 跨用户死代码清理 | `OperationLogDao.kt` | 数据隔离收尾 |
| 3 | App 签名自校验 | `AppSignatureGuard.kt`、`FunLifeApplication.kt`、`build.gradle.kts` | 防止重打包 |
| 4 | `coin_log` 客户端 nonce + ts | `CoinCloudReporter.kt` | 防重放 |
| 5 | `tokenHealthy` StateFlow + 启动检查 | `AuthViewModel.kt` | 暴露 token 健康度 |
| 6 | 首页"安全升级"一次性弹框 | `HomeScreen.kt` | 引导老用户重新登录补 token |
| 7 | AUTH 错误日志 | `CoinCloudReporter.kt` | 诊断 token 失效 |

---

## 4. 后端红队测试（自动化）

### 4.1 同设备多账号刷卡密（第一轮）

| ID | 场景 | 期望 | 实际 | 结果 |
|---|---|---|---|---|
| T1.1 | BETA 码 user1@A 首激活 | `ok:true` | `ok:true,isReissue:false` | ✅ |
| T1.2 | BETA 码 user1@A 重发 | `isReissue:true` | 同上 | ✅ |
| T1.3 | BETA 码 user2@A 换账号 | `USED` | `USED` | ✅ |
| T1.4 | BETA 码 user1@B 换设备 | `USED` | `USED` | ✅ |
| T1.5 | VIP 码 uid1001@A 首兑换 | `ok+bonusCoins>0` | `bonusCoins:100` | ✅ |
| T1.6 | VIP 码 uid1001@A 重发 | `bonusCoins:0` | `bonusCoins:0,isReissue:true` | ✅ |
| T1.7 | VIP 码 uid2002@A 换账号 | `USER_MISMATCH` | `USER_MISMATCH` | ✅ |
| T1.8 | VIP 码 uid1001@B 换设备 | `USED` | `USED` | ✅ |
| T1.9 | VIP 码 uid0@A 游客 | `USER_REQUIRED` | `USER_REQUIRED` | ✅ |

### 4.2 coin_log 鉴权与防重放（第二轮）

| ID | 场景 | 期望 | 实际 | 结果 |
|---|---|---|---|---|
| A1 | 合法 token + 正常 nonce | `ok:true` | `ok:true,flags:[]` | ✅ |
| A2 | 同 nonce 第二次提交 | `REPLAY` | `REPLAY` | ✅ |
| A3 | ts 偏移 -30 分钟 | `TS_OUT_OF_WINDOW` | 同上 | ✅ |
| A4 | user1 的 token 给 user2 用 | `AUTH_USER_MISMATCH` | 同上 | ✅ |
| A5 | 同 token 换设备 | `AUTH_DEVICE_MISMATCH` | 同上 | ✅ |
| A6 | 无 token | `AUTH_REQUIRED` | 同上 | ✅ |
| A7 | 伪造 token | `AUTH_INVALID` | 同上 | ✅ |

### 4.3 VIP 凭证 verify

| ID | 场景 | 期望 | 实际 | 结果 |
|---|---|---|---|---|
| B1 | 合法 cert | `ok:true` 续期 | 续期成功 | ✅ |
| B2 | 篡改 `vipLevel`，签名不变 | `BAD_SIGNATURE` | 同上 | ✅ |
| B3 | `cert.exp < now` | `CERT_EXPIRED` | 同上 | ✅ |
| B4 | 设备级吊销 + 老 cert | `REVOKED` | 同上 | ✅ |
| B5 | 取消吊销 | `ok:true` 续期 | 同上 | ✅ |

### 4.4 设备迁移 migrate

| ID | 场景 | 期望 | 实际 | 结果 |
|---|---|---|---|---|
| C1 | 无 token | `AUTH_REQUIRED` | 同上 | ✅ |
| C2 | 假 token | `AUTH_INVALID` | 同上 | ✅ |
| C3 | 缺 `oldDeviceId` | `OLD_DEVICE_REQUIRED` | 同上 | ✅ |
| C4 | `oldDeviceId` 错 | `OLD_DEVICE_MISMATCH` | 同上 | ✅ |

---

## 5. 客户端编译与静态检查

| 任务 | 结果 |
|---|---|
| `:app:assembleDebug` | ✅ BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` | ✅ 通过（项目暂无单元测试） |
| `:app:lintDebug` | ⚠️ 1 处历史告警（`AnniversaryNotificationManager.kt:116`，与本次修复无关） |

---

## 6. ⚠️ 必须人工真机/模拟器验证的清单

### 6.1 优先级 P0（涉及本次安全修复的副作用，**必须先验**）

| ID | 步骤 | 期望 | 通过？ |
|---|---|---|---|
| M1 | 安装 debug 包，正常注册新账号 | 注册成功；logcat 显示 `registerLog` 返回 `ok:true,deviceToken=...` | ☐ |
| M2 | 触发任意金币变动（转盘、习惯打卡等） | logcat **无** `CoinCloudReporter: device_token rejected` | ☐ |
| M3 | 用老账号（升级前已存在）首次进入 App | 首页弹"**安全升级**"对话框 | ☐ |
| M4 | M3 弹框点"立即重新登录" | App 重启到登录页，账号数据保留 | ☐ |
| M5 | M4 之后重新登录 | 弹框不再出现；金币变动 logcat 无 AUTH 错误 | ☐ |
| M6 | M3 弹框点"稍后" | 弹框关闭；下次冷启动不再弹 | ☐ |
| M7 | 重新打开/关闭 App 多次 | 弹框只出现一次（per 账号） | ☐ |
| M8 | 切换到另一个 token 缺失的老账号 | 该账号也弹一次"安全升级" | ☐ |

### 6.2 优先级 P1（多账号数据隔离）

| ID | 步骤 | 期望 | 通过？ |
|---|---|---|---|
| ISO1 | 账号 A 计分页加 3 玩家、上分若干 | 显示 A 数据 | ☐ |
| ISO2 | 退出登录，注册账号 B，进计分页 | **空白**，看不到 A 玩家 | ☐ |
| ISO3 | 账号 B 加自己玩家上分，切回 A | A 看不到 B 数据 | ☐ |
| ISO4 | A 与 B 习惯打卡日历 | 互不可见 | ☐ |
| ISO5 | A 与 B 宠物状态 | 互不可见 | ☐ |
| ISO6 | A 与 B 金币余额 | 互相独立 | ☐ |
| ISO7 | A 与 B 心情记账 | 互不可见 | ☐ |
| ISO8 | A 与 B 纪念日 | 互不可见 | ☐ |
| ISO9 | A 与 B 倒计时目标 | 互不可见 | ☐ |
| ISO10 | A 与 B 心愿单 | 互不可见 | ☐ |

### 6.3 优先级 P2（金币防作弊）

| ID | 步骤 | 期望 | 通过？ |
|---|---|---|---|
| C1 | 转盘抽奖：幸运转盘 | **不**赠送金币 | ☐ |
| C2 | 转盘抽奖：商品转盘 | 按设计扣分 | ☐ |
| C3 | 习惯打卡 → 取消 → 再打卡 | **第二次不再发金币** | ☐ |
| C4 | 喂宠物多次直到日任务完成 | 之后再喂**不再加币** | ☐ |
| C5 | 修改系统时间到明天 | 习惯打卡判定**不被欺骗** | ☐ |
| C6 | 卸载重装 App | VIP 状态联网恢复，金币重置（设计如此） | ☐ |

### 6.4 优先级 P3（VIP 流程）

| ID | 步骤 | 期望 | 通过？ |
|---|---|---|---|
| V1 | 兑换 VIP 码 | 兑换成功，获得 bonusCoins | ☐ |
| V2 | 退出登录 → 重新登录 | VIP 状态保留 | ☐ |
| V3 | 同设备另一个账号尝试同 VIP 码 | 拒绝（`USER_MISMATCH`） | ☐ |
| V4 | 卸载重装后联网 | 自动恢复 VIP（verify 续 cert） | ☐ |
| V5 | VIP 激活动画 | 仅当前账号首次激活时显示 | ☐ |
| V6 | 设备迁移流程 | 需要原设备 + 目标设备验证 | ☐ |

### 6.5 优先级 P4（其它功能冒烟）

| 模块 | 关键场景 | 通过？ |
|---|---|---|
| 纪念日 | 增删改查、置顶、提醒 | ☐ |
| 倒计时 | 新建、修改、删除 | ☐ |
| 心愿单 | 新建愿望、完成、删除 | ☐ |
| 习惯打卡 | 新建习惯、打卡、统计 | ☐ |
| 计分计数器 | 多玩家加减分、历史 | ☐ |
| 转盘 | 自定义选项、抽奖、动画 | ☐ |
| 宠物 | 喂养、阶段成长、动画 | ☐ |
| 商店 | 浏览、购买、消耗金币 | ☐ |
| 心情记账 | 新建账单、统计、解析 | ☐ |
| 头像/昵称 | 修改、上传 | ☐ |
| 主题切换 | 浅色/深色 | ☐ |
| 字体设置 | 大小、字体 | ☐ |
| 备份恢复 | 导出、导入 | ☐ |

---

## 7. 已知风险与剩余安全任务（未实施）

| # | 项 | 严重度 | 状态 |
|---|---|---|---|
| #1 | Root 检测 | 🟡 中 | 未做 |
| #2 | 反调试检测 | 🟡 中 | 未做 |
| #4 | Room 数据库 SQLCipher 加密 | 🟡 中 | 未做 |
| #5 | Cert pin 备份指纹 | 🟢 低 | 未做 |
| #9 | 服务端审计日志 TTL | 🟢 低 | 未做 |
| #13 | 多端同账号主动踢出旧端 | 🟡 中 | 未做 |

---

## 8. 测试执行方式

### 自动化（已跑过，可复跑）
```powershell
# 后端红队测试（需要 tcb cli + cloudbaserc.json）
cd d:\soft\backend
# _redteam_run2.ps1 在第二轮测试时已使用并清理；如需复跑请按下文重建临时 redteam 函数
```

### 客户端
```powershell
# 全量编译验证
cd d:\soft
.\gradlew.bat :app:assembleDebug

# 单元测试 + lint
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --console=plain
```

### 真机/模拟器
- 安装 `app\build\outputs\apk\debug\app-debug.apk`
- 按 §6 清单逐项验证，勾选"通过"列
- 任一失败：抓 logcat 提交给开发

---

## 9. 签字

- 后端自动化测试：**Cascade** ✅ 2026-05-26
- 客户端真机测试：**☐ 待执行**
- 上线放行：**☐ 待签字**
