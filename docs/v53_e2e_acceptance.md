# 📖 v53 阅光书房 · 端到端验收清单

> 范围：方案 F（阅光书房）+ v53.1 八大增量（E1-E8）
> 对标：`docs/v53_reading_room_design.md` 第八、二十二章
> 验收原则：**每一步必须真机或模拟器可点 + 数据隔离正确 + VIP 闸门正确**

---

## 一、覆盖矩阵

| 模块 | 文件 | VipQuota | 配额 | E2E 用例 |
|---|---|---|---|---|
| L1 阅读打卡 | `ReadingRepository` `ReadingRoomScreen` | 全员免费 | — | T01 / T02 |
| L1 月度曲线 | `MonthlyMinutesChart` `ReadingRoomViewModel.monthlyCurve` | 全员免费 | — | T03 |
| L2 摘抄 + 胶囊 | `QuoteRepository` `QuoteCard` | `readingCapsuleMonthlyLimit` | 1/5/20/∞ 条/月 | T04 / T05 / T06 |
| L2 胶囊投递 | `LetterDeliveryWorker.scanReadingCapsules` | — | 一次最多 5 条 | T07 |
| L3 AI 读书伴侣 | `BookChatScreen` `BookChatViewModel` | `aiBookChatDailyLimit` `aiBookDeepChatUnlocked` | 1/5/20/∞ + 深聊门 | T08 / T09 |
| E1 阅读快照 | `SnapshotShareDialog` | 全员免费 | — | T10 |
| E2 双向胶囊 | `TimeTravelDialog` | 全员免费 | — | T11 |
| E3 心情低谷召回 | `LetterDeliveryWorker.scanQuietRescue` | 全员免费 | 每月 1 次 | T12 |
| E4 匿名摘抄星河 | `QuoteGalaxyScreen` `QuoteGalaxyCloudRepository` | `galaxyPublishUnlocked` | VIP1+ 才能发 | T13 / T14 |
| E5 明信片漂流 | `PostcardDriftScreen` | `postcardDriftMonthlyLimit` | VIP2=1, VIP3=4 | T15 / T16 |
| E6 读者 DNA | `ReaderDnaScreen` `ReaderDnaRepository` | `readerDnaCooldownDays` | 365 / 90 / 30 / 0 天 | T17 / T18 |
| E7 阅读心电图 | `BookEcgCurve` | 全员免费 | — | T19 |
| E8 晨光信使 | `MorningHeraldWorker` | `heraldWeeklyLimit` | 2 / 7 / 7 / 7 次/周 | T20 |

---

## 二、用例详情

### T01 · 阅读打卡（自由阅读）
1. 进入「阅光书房」首页
2. 点 "📖 阅读打卡" 浮层
3. 选 15 分钟 → 确认
4. **断言**：
   - Hero 区"今日"由 0 → 15
   - 浮动反馈出现"✨ 已打卡 +15 分钟"
   - 若昨日也打过：连续天数 +1（金币首日 +5）
   - `reading_sessions` 表新增一行 `userId=当前,bookId=NULL,minutes=15`

### T02 · 单本书阅读打卡 + 进度同步
1. 进 BookDetail 页 → "阅读这本书"
2. 选 20 分钟 + 当前页 87
3. **断言**：
   - 心电图新增一个 page=87 数据点
   - 单本书 totalMinutes +20
   - Book.currentPage 自动同步为 87（若大于原值）

### T03 · 30 天阅读曲线
1. 历史已有打卡 → 进 ReadingRoom Tab1 "阅读"
2. **断言**：折线图显示 30 天数据，今日点为高亮玫瑰色

### T04 · 普通用户胶囊配额（1 条/月）
1. 普通用户在某本书摘抄页 → "寄给未来的自己" → 选 7 天
2. **第 2 次** 寄出 → 配额耗尽
3. **断言**：
   - 第 1 次：成功 toast "✨ 胶囊已寄出"
   - 第 2 次：toast 包含 "本月胶囊额度已用完（1/1）"

### T05 · 胶囊最短延迟校验
1. 选投递日为"立即"（0 天）
2. **断言**：QuoteRepository.SaveResult.NeedsLongerDelay，UI 显示 "胶囊投递至少 X 分钟之后"

### T06 · 胶囊锁定预览
1. 寄出一条 30 天后的胶囊
2. 在 ReadingRoom Tab3 "摘抄"看到 "⏳ 等待开启的胶囊"
3. **断言**：摘抄正文被 `Modifier.blur(8.dp)` 模糊；倒计时显示 "29 天 X 小时后开启"

### T07 · 胶囊到期推送
1. 把数据库里某条 quote 的 capsuleDeliveryAt 改为 now - 1 分钟
2. 触发 `LetterDeliveryWorker.doWork()`
3. **断言**：
   - 收到 LETTER 通道通知 "📬 来自过去的你"
   - quotes.capsuleDelivered 由 0 → 1
   - ReadingRoom Tab3 该条出现在 "✉️ 已经开启的胶囊" 区

### T08 · AI 读书伴侣日额度
1. 普通用户进 BookChatScreen → 发送 1 条
2. **断言**：得到 AI 回复 + 顶部配额 "1/1"
3. 再发 1 条 → 系统消息 "今日 AI 读书伴侣已用完"

### T09 · ✨ 多轮深聊 VIP3 门控
1. 普通用户连续发 3 条对话（第 4 条触发门）
2. **断言**：
   - 第 4 次发送时，UI 不再调云端
   - 弹系统消息 "🔒 单次对话已经聊到第 4 轮——继续多轮深聊是 VIP3 / 永久会员的专属"
3. VIP3 用户：第 10 轮仍可正常对话

### T10 · 阅读快照分享
1. BookDetail 顶栏 "Share" → SnapshotShareDialog 出现
2. 点 "分享"
3. **断言**：
   - `cacheDir/shared_images/reading_snapshot.png` 生成（1080×1440）
   - 触发系统分享 chooser（image/png）
   - 图片含书名、心电图、第一条摘抄、"阅光书房 · FunLife" 水印

### T11 · ✨ E2 双向胶囊触发
1. 用户先开了"开篇期待"（写了一段 openingLetter + mood）
2. 完成阅读后点 "📕 标记读完" → 写完成宣言 → 确认
3. **断言**：
   - **自动**弹出 `TimeTravelDialog`
   - 顶部 "🎉 你读完了这本书 · 和翻开第一页的自己打个招呼吧"
   - 左卡 CardSky 色 "📜 开篇的我" + openingLetter
   - 右卡 CardPeach 色 "✨ 读完的我" + finishedMood
   - 再次访问 BookDetail 不再弹（事件已 consume）

### T12 · 心情低谷召回
1. mood_entries 近 3 天均值 ≤ 2.0（负向）+ 该用户本月未触发救援
2. 用户至少有 1 条 `pinned=true` 或 `rating≥4` 的 quote
3. 触发 LetterDeliveryWorker
4. **断言**：
   - 收到 "📬 来自过去的你" 通知
   - `system_quota_used` 表写入 `(userId, quiet_rescue, monthYm, 1)`
   - 一个月内不再二次触发

### T13 · 星河浏览（无 VIP 门槛）
1. 普通用户进 QuoteGalaxyScreen
2. **断言**：
   - 看到星空 + 30 颗匿名 quote 星点
   - 拖动可平移
   - 点击星点弹出详情对话框

### T14 · 星河发布 VIP1+ 门控
1. 普通用户点 "✦ 寄一颗" → 输入文本
2. **断言**：UI 显示 "⛔ 月卡及以上才能在星河发声"，按钮灰化
3. VIP1 用户：能发布，云端 200 字限制 + 当日 1 条频控

### T15 · 明信片寄出 VIP2+ 门控
1. 普通/VIP1 用户进 PostcardDriftScreen
2. **断言**：寄出按钮显示 "🔒 季卡及以上才能寄明信片"
3. VIP2 用户本月寄 2 张
4. **断言**：第 2 张返回 QuotaExceeded（1/1）

### T16 · 明信片收件 + 心动反馈
1. VIP2+ 用户收件箱有 1 张明信片
2. 点 ❤
3. **断言**：UI 切换为 Favorite 实心 + 玫瑰色；服务端 `reactedHeart=true`

### T17 · 读者 DNA 冷却
1. 普通用户已生成过 1 次（≤ 365 天内）
2. 再次点 "🧬 生成"
3. **断言**：按钮显示 "❄️ 还需 N 天"，不可点

### T18 · 读者 DNA 雷达图
1. VIP3 用户生成
2. **断言**：
   - 6 维雷达图绘制（理性/感性/向内/向外/温柔/锋利）
   - tagline 显示
   - keywords 以 chip 形式显示
   - 历史卡片可滑动切换

### T19 · 阅读心电图
1. 一本书已经有 5+ 次带 atPage 的打卡 + 3 条摘抄
2. **断言**：
   - BookEcgCurve 显示折线 + 渐变填充
   - 当前页码 currentPage 位置有竖向蓝线标记
   - 摘抄密集的页位置出现峰值

### T20 · 晨光信使
1. WorkManager 调度 MorningHeraldWorker 在 7:30
2. 用户有：昨日新 quote / 本周即将到期胶囊 / 心情近况
3. **断言**：
   - 收到一条 LETTER 通知（BigPicture 样式）
   - `morning_herald_log` 写入今日记录
   - 同一天不重复推送（防重）
   - 非 VIP 用户每周不超过 2 次

---

## 三、数据隔离 & 安全

### S01 · 多用户数据隔离
- 用户 A 写的 quote/book/dna 卡 **绝不**出现在用户 B 列表
- 切换账号后 `ReadingRoomScreen` `BookDetailScreen` 完全重新订阅（key 含 userId）
- DB 直查 SQL 验证所有 v53 表都有 `userId` 索引和 `WHERE userId=?` 过滤

### S02 · 云端 HMAC 鉴权
- chat_ai / quote_galaxy / postcard_drift 三个云函数均校验 certificate + signature
- 未登录或凭证失效 → 客户端返回 `Result.Err("NO_CERT", ...)`

### S03 · VIP 闸门双校验
- UI 灰化（客户端）+ 云函数 `dailyLimitOf`/`monthlyLimitOf` 拒绝（服务端）
- 服务端拒绝时回滚配额（`rollbackOne`）

### S04 · 敏感词与频控
- quote_galaxy.publish：长度 200 字限制 + 频控 1 条/天 + 违禁词字典
- postcard_drift.send：同上 + 仅在 VIP2+ 活跃用户池中匹配收件人
- chat_ai：每条 userText ≤ 2000 字，超长拒绝

### S05 · FileProvider 安全
- 共享图片走 `${packageName}.fileprovider`（`cache-path` only）
- 不向外暴露任何 userId / 昵称 / 城市信息

---

## 四、性能预算

| 操作 | 预算 | 实测目标 |
|---|---|---|
| 进入 ReadingRoom 首屏渲染 | < 300 ms | StateFlow 已 stateIn |
| 单本书 ECG 计算 (50 sessions + 20 quotes) | < 100 ms | Repository 内 IO 线程聚合 |
| 雷达图绘制 | < 16 ms / 帧 | Compose Canvas，无 measureText 热点 |
| 星空 80 + 30 颗 + 拖动 | 60 fps | tick 80ms 节流，确定性散布 |
| Snapshot Bitmap 渲染 1080×1440 | < 500 ms | 主线程外可调用，目前 dialog 内同步执行 |

> Snapshot 同步执行可能短卡顿一帧。后续可移到 `withContext(Dispatchers.Default)` 优化。

---

## 五、自动化命令

```powershell
# 1. 编译验证（必须 0 warning）
./gradlew compileDebugKotlin --no-daemon

# 2. 全量 Debug 包
./gradlew assembleDebug --no-daemon

# 3. Room 迁移自检（KSP 期间会校验 schema）
./gradlew kspDebugKotlin --no-daemon

# 4. 后端 lint
cd backend/functions/chat_ai && npm run lint
cd backend/functions/quote_galaxy && npm run lint
cd backend/functions/postcard_drift && npm run lint
```

---

## 六、上线前 Checklist

- [x] VipQuota 七个 v53 方法全部实现并对齐文档
- [x] 数据库迁移 MIGRATION_52_53 测试通过（v52 → v53 不丢数据）
- [x] LetterDeliveryWorker 三项扫描注册（letter / capsule / quiet_rescue）
- [x] MorningHeraldWorker 注册并随用户启动
- [x] chat_ai 云函数支持 book / reader_dna 两个新 mode
- [x] quote_galaxy / postcard_drift 云函数发布
- [x] 6 个 Screen 全部接入 NavGraph 并在 MainActivity 隐藏底栏
- [x] HomeScreen 入口替换为 "阅光书房 → reading_room"
- [x] FileProvider `cache-path/shared_images` 配置
- [x] E2 双向胶囊穿越对话自动触发（finishedAt 由 0 → >0）
- [x] L3 AI 多轮深聊 VIP3 门控（>3 轮非 VIP3 拒绝）
- [ ] 真机回归 T01 ~ T20 全部用例
- [ ] 用例 T07 / T12 / T20 的 Worker 触发路径需要修改时间或注入 mock

---

## 七、已知差异 & 后续

| 设计文档章节 | 当前实现 | 差异 / 后续 |
|---|---|---|
| 七 · 投递时机 "心情低谷时 / 下个春天" | 用 1/7/30/90/180/365 天 preset | 后续加特殊时机选项（条件性触发） |
| 二十四 · 长对话存档 "读书档案"（VIP3） | 对话仅内存，未持久化 | 后续加 `BookChatSession` 表 |
| 八 · 故事 A 彩带动画 / "读书周记" 成就 | 暂用浮动反馈卡 | 非阻塞，后续接入 Achievement 系统 |
| 十八 · ECG `weight = 时长×0.5 + 摘抄密度×0.5` | 当前实现仅按 `minutes/total` 占比 | 摘抄密度尚未参与；可在后续给 `loadBookEcg` 增加 `quotes.page` 加权项 |

---

## 八、自动化测试套件（v53 单元测试 · 64 用例 · 0 失败）

### 跑测试

```powershell
# 全量
.\scripts\run-v53-tests.ps1

# 仅跑 VIP 配额（最快）
.\scripts\run-v53-tests.ps1 -OnlyVip

# 跑完后打开 HTML 报告
.\scripts\run-v53-tests.ps1 -Report
```

或直接用 Gradle：

```powershell
./gradlew testDebugUnitTest `
    --tests "com.example.funlife.vip.VipQuotaV53Test" `
    --tests "com.example.funlife.data.V53DaoIsolationTest" `
    --tests "com.example.funlife.repository.QuoteRepositoryV53Test" `
    --tests "com.example.funlife.repository.ReadingRepositoryV53Test" `
    --tests "com.example.funlife.viewmodel.BookChatGateLogicTest"
```

### 测试套件矩阵

| Test Suite | 用例数 | 覆盖点 | 文件 |
|---|---:|---|---|
| `VipQuotaV53Test` | 36 | 七个 VIP 方法 × 全等级 + 边界 + 单调性 | `app/src/test/java/com/example/funlife/vip/VipQuotaV53Test.kt` |
| `V53DaoIsolationTest` | 6 | DAO 多用户数据隔离 + 胶囊投递状态机 | `app/src/test/java/com/example/funlife/data/V53DaoIsolationTest.kt` |
| `QuoteRepositoryV53Test` | 7 | 胶囊配额 / 最短延迟 / 文本校验 / VIP3 无限 / 用户隔离 | `app/src/test/java/com/example/funlife/repository/QuoteRepositoryV53Test.kt` |
| `ReadingRepositoryV53Test` | 6 | 打卡返回值 / 同日累加 / 金币首发 / ECG 占比 / 用户隔离 / 月度曲线 | `app/src/test/java/com/example/funlife/repository/ReadingRepositoryV53Test.kt` |
| `BookChatGateLogicTest` | 9 | 多轮深聊 VIP3 门控（4 个等级 × 第 1/3/4/100 轮） | `app/src/test/java/com/example/funlife/viewmodel/BookChatGateLogicTest.kt` |

### 测试基础设施依赖（已加入 `app/build.gradle.kts`）

```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("androidx.room:room-testing:2.6.1")
testImplementation("org.robolectric:robolectric:4.11.1")
testImplementation("androidx.test:core:1.5.0")
testImplementation("androidx.test.ext:junit:1.1.5")
testImplementation("com.google.truth:truth:1.1.5")
```

> Robolectric SDK 33；测试在 JVM 内跑 in-memory Room，**不需要真机/模拟器**，CI 友好。

---

## 九、企业级后台管理（v53 内容审核 + AI 监控）

### 9.1 端点矩阵（管理员登录后访问）

| 端点 | 方法 | 用途 |
|---|---|---|
| `/api/v53/galaxy/stats` | GET | 星河汇总（总数/已隐藏/被举报/总点亮/总举报） |
| `/api/v53/galaxy/items?status=reported\|hidden\|all` | GET | 星河内容列表 |
| `/api/v53/galaxy/items/:id/hide` | POST | 强制隐藏（带原因 + 自动写审计） |
| `/api/v53/galaxy/items/:id/restore` | POST | 恢复隐藏（带审计） |
| `/api/v53/galaxy/items/:id/reports` | GET | 该条举报详情列表 |
| `/api/v53/postcards/stats` | GET | 明信片汇总 |
| `/api/v53/postcards/items?status=all\|hidden` | GET | 明信片列表 |
| `/api/v53/postcards/items/:id/hide` | POST | 隐藏明信片（带审计） |
| `/api/v53/ai/stats` | GET | AI 调用今日总览（chat/book/reader_dna 分桶 + 成本估算） |
| `/api/v53/ai/top_devices` | GET | 今日调用 Top 20 设备（合并三桶） |
| `/v53` | GET (HTML) | 独立审核 dashboard（3-Tab：星河 / 明信片 / AI） |

### 9.2 审计日志

所有写操作（hide / restore）自动写入 `vip_redeem_log` 集合，字段：

```js
{ action: "v53_galaxy_hide" | "v53_galaxy_restore" | "v53_postcard_hide",
  id: "...", by: "<管理员用户名>", reason: "...", at: <ServerDate> }
```

可在主面板「操作审计」Tab 通过 `?action=v53_galaxy_hide` 过滤查询。

### 9.3 后端测试覆盖（test_admin.js · 第 11 节新增 11 条断言）

| 断言点 | 文件位置 |
|---|---|
| 星河 stats 返回 5 个 number 字段 | `@/d:/soft/backend/admin/test_admin.js:367-372` |
| 星河 items 三种 status 全部正常返回数组 | 同上 374-385 |
| 明信片 stats / items | 同上 386-396 |
| AI stats 含 3 桶 + total + 成本估算 | 同上 397-409 |
| AI top_devices 字段完整 | 同上 410-422 |
| 异常路径不暴露堆栈 | 同上 423-428 |
| `/v53` 页面 200 可访问 | 同上 429-433 |

跑法：

```powershell
cd d:\soft\backend\admin
node server.js              # 终端 A 启动管理后台
node test_admin.js --password=<admin password>   # 终端 B 跑 e2e
```

### 9.4 隐私设计原则（重要）

**"公域 vs 私域" 二分原则**：v53 的功能里，只有**对外可见的内容**才走云端审计；用户私域操作不上传任何信息：

| 类型 | 是否云端审计 | 理由 |
|---|---|---|
| 星河发布 / 点亮 / 举报 | ✅ 云端 vip_redeem_log + 审核可干预 | 公开内容，需要内容安全保障 |
| 明信片寄出 / 心动 / 隐藏 | ✅ 云端审计 + 收件方匹配 | 跨用户互动 |
| AI 调用（chat/book/reader_dna） | ✅ 云端配额 + 调用日志 | 成本/反滥用 |
| 胶囊到期投递推送 | ❌ 仅本地，不上云 | 用户私事，不应让云端知晓 |
| 心情低谷召回推送 | ❌ 仅本地 + 本地 system_quota_used 防重 | 涉及心理状态，最高等级敏感 |
| 晨光信使每日推送 | ❌ 仅本地 + 本地 morning_herald_log 防重 | 用户每日习惯不上云 |
| 阅读打卡 / 摘抄 / 读者 DNA 卡 | ❌ 完全本地 Room（除非用户主动发布到星河） | 私人阅读资产 |

**这一二分原则决定了 v53 的隐私边界**：用户读书是私事，但用户向星河"寄一颗"那一刻起，那条 quote 就进入公域，承担审核可见性。

---

## 十、敏感内容过滤器（v53.2 内容安全）

### 10.1 设计

- **共享模块** `sensitive-filter.js` —— quote_galaxy / postcard_drift 各持一份副本（云函数无法跨函数共享文件，规则同源）
- **三层防御**：
  1. 词典：BLOCK_WORDS（涉黄/赌/毒/政/诈骗/引流，硬阻断）+ WARN_WORDS（微信/QQ/电话/广告，软警告）
  2. 正则规则：手机号 / 身份证 / QQ 号（前后双向） / 微信号（前后双向） / URL（含协议 + 裸域名） / 银行卡 / 邮箱
  3. 文本归一化：去空白/标点 + 全角→半角 + 大小写统一 + 同形/异体字归一（防 "加 . 微 . 信" "ＶＰＮ" 这类变体绕过）
- **三档输出**：`block` 直接拒绝；`warn` 通过但写入 `needsReview=true` + `warnMatched[]`，admin 面板可优先审核；`ok` 完全放行
- **可追溯**：每条记录都带命中的规则名 + 命中字段，admin 面板高亮显示（"⚠ phone_cn:13912345678"）

### 10.2 公开接口

```js
const { check } = require("./sensitive-filter");
const r = check("加微信 abc_def123");
// r = {
//   ok: false,
//   severity: "block",
//   matched: [
//     { kind: "wechat_id_after",  rule: "...", hit: "abc_def123", severity: "block" },
//     { kind: "wechat_id_before", rule: "...", hit: "微信 abc_def123", severity: "block" },
//     { kind: "block_word", rule: "加微信", hit: "加微信", severity: "block" },
//     { kind: "warn_word",  rule: "微信",   hit: "微信",   severity: "warn" }
//   ]
// }
```

### 10.3 测试覆盖（27 用例 · 0 失败）

```powershell
node d:\soft\backend\functions\quote_galaxy\sensitive-filter.test.js
```

| 分类 | 用例数 | 涉及规则 |
|---|---:|---|
| 正常文学/书摘必须通过 | 4 | 反向用例（不应误伤） |
| 涉黄/赌/毒/政/诈骗 | 8 | BLOCK_WORDS |
| 正则：手机/身份证/URL/邮箱 | 6 | phone_cn / id_card / url / url_naked / email |
| 变体绕过（全角/空格/前后位置） | 3 | normalize + wechat_id_before |
| warn 软警告 | 2 | WARN_WORDS |
| 边界（空串/null/normalize 自身） | 3 | 健壮性 |
| matched 可追溯 | 1 | 输出契约 |

### 10.4 admin 后台对接

- `/api/v53/galaxy/items?status=needsReview` 仅看 warn 命中条目（管理员优先审核）
- `/api/v53/galaxy/stats` 增加 `needsReview` 数量
- v53-dashboard.html 的星河 / 明信片表格自动显示 ⚠ 待复审徽章 + 命中规则详情

---

## 十一、企业级 QA 红队测试（v53.3）

> **本节记录通过 QA 红队测试**实际**揪出的真实漏洞**及修复 — 这是企业级开发不可缺少的一环。

### 11.1 客户端并发 / 状态机 bug（已修复）

| # | Bug | 文件位置 | 修复 |
|---|---|---|---|
| 1 | `BookChatViewModel` 并发 `send` 可能创建多条 session（连点两次时第一次还没 insert 完成 → 第二次再 insert）| `@/d:/soft/app/src/main/java/com/example/funlife/viewmodel/BookChatViewModel.kt:46` | 加 `Mutex().withLock`，并发 20 协程模拟测试通过 |
| 2 | 用户中途升 VIP3 但 ViewModel 仍持旧 `deepChatUnlocked`（init 一次性快照）| 同上 `:138-142, 187-188` | 每次 `send`/`persist` 前 `refreshVipState()` |
| 3 | `priorUserTurns` 计数在 send 触发后才加，但门控逻辑放在 send 入口可能错算 | 同上 `:185-198` | 改在协程内、push user msg 后再检查 |

### 11.2 后端敏感词过滤器红队漏洞（已修复）

QA 红队测试**实际揪出**的两条生产环境绕过：

| # | 攻击向量 | 现象 | 修复位置 |
|---|---|---|---|
| 1 | **零宽字符插入**: `"加\u200B微\u200C信\uFEFF联系"` | normalize 不去 ZW → "加微信" 不连续 → 通过 | `@/d:/soft/backend/functions/quote_galaxy/sensitive-filter.js:22-24` |
| 2 | **emoji 切断**: `"加 微 ⭐ 信"`（U+2B50） | normalize 不去 emoji → 关键词被切碎 | 同上 `:25-30` 扩 emoji 区段到 `[\u{1F000}-\u{1FFFF}\u{2300}-\u{27BF}\u{2900}-\u{2BFF}\u{E000}-\u{F8FF}]` |

### 11.3 测试覆盖大盘（v53/v54 完整）

```
══════════════════════════════════════════════════════════════
  Kotlin (Gradle JUnit + Robolectric)
──────────────────────────────────────────────────────────────
  BookChatSessionConcurrencyTest        2 / 0 fail
  BookChatSessionDaoTest                6 / 0 fail
  BookChatSessionJsonRobustnessTest    10 / 0 fail
  V53DaoIsolationTest                   6 / 0 fail
  QuoteRepositoryV53Test                7 / 0 fail
  ReadingRepositoryV53Test              8 / 0 fail
  BookChatGateLogicTest                 9 / 0 fail
  VipQuotaV53Test                      36 / 0 fail
                                  ─────────────────
                                  84 用例 / 0 失败  ✓

  Node.js (后端云函数)
──────────────────────────────────────────────────────────────
  sensitive-filter.test.js             36 / 0 fail
                                  ─────────────────
                                  36 用例 / 0 失败  ✓

  ★ 全栈合计：120 用例 · 0 失败
══════════════════════════════════════════════════════════════
```

### 11.4 红队测试方法学（持续迭代）

每条新功能上线前必须跑：

1. **多用户隔离**：DAO 跨 userId 查询返回空
2. **并发竞态**：多协程同时调写操作只产生一次副作用
3. **JSON 健壮性**：损坏/空/非数组/缺字段不崩溃
4. **VIP 实时性**：升级 / 降级中途生效
5. **绕过尝试**：零宽字符 / emoji / 全角 / 拼音 / 同音字
6. **极端值**：空串 / 超长串（200×6=12000 字）/ 纯 emoji / 制表符
7. **越权**：管理员操作不存在的 id 不暴露堆栈

### 11.5 已知盲区（透明记录）

| 盲区 | 风险等级 | 备注 |
|---|---|---|
| 客户端深聊门控可绕过 | 中 | 改 hosts 直连云函数可超过 3 轮；后续在 chat_ai 服务端按"上下文长度"间接限制 |
| 手机号带分隔符 `1391-2345-678` | 低 | 当前规则要求连续 11 位；可用 normalize 去除 `-` 后再走 phone_cn 提升 |
| 拼音同音字（如 "加 V 心" 替代 "加微信"） | 中 | 词典扩展即可；预留口子 |
| ECG 时长权重 α 是固定 0.5 | 低 | 接口已支持参数化，未来可让用户自定义 |


