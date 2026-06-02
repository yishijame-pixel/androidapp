# 📖✨ 阅光书房（Reading Room）—— v53 产品设计方案

> 「人生书架」从纯清单进化为**funlife 主线下的"阅读情感容器"**：
> 一本书 = 一个时光胶囊 = 一位 AI 读书伙伴 = 一次习惯打卡
>
> 整套设计 0 新增基础设施，**全部复用现有系统**：
> 习惯打卡 / 时光信箱 / chat_ai 云函数 / VIP 配额 / 金币奖励

---

## 一、产品定位 · 一句话

> 「阅光书房」是你**和书的私人时间博物馆**。它记录你读过的每一本，
> 让你的摘抄变成给未来自己的礼物，让你的疑问变成与 AI 的深夜对谈。

放弃和豆瓣比"找书"，放弃和微信读书比"读书"，**只做"读完之后"那一段**——这是市面上没人做透的。

---

## 二、三层价值阶梯

```
                  L3 · 陪伴 ───────────────────────── 🤖 AI 读书伴侣
                  (VIP3/Permanent 专享，月卡限额)     深聊一本书，多轮对话，可保存为长对话档案

                  L2 · 情感 ─────────────────── 💌 摘抄时光胶囊
                  (VIP1+ 解锁，无限/每月 N 条)   "这句话寄给 3 个月后的我"
                                                到期日推送通知 + 信箱式精美呈现

                  L1 · 工具 ─────────── ⏱ 阅读打卡 + 📚 书目记录
                  (全员免费)              今天读了 X 分钟 / 连续 N 天 / 金币奖励
                                          书目 CRUD（现有）
```

每一层都**单独成立、独立闭环**，叠加使用价值倍增。

---

## 三、核心场景（用户故事）

### 故事 A · "我今天读了 30 分钟，连签 14 天"
1. 小白点开「📖 阅光书房」，看到首页大字：**"连续阅读 13 天 🔥"**
2. 点 **"今天读了多久"** → 选 30 分钟 → 立即 +5 金币，连续 14 天彩带动画 + 解锁"读书周记"成就
3. 进入"我在读" tab，看到正在读的《活着》，点进去更新阅读进度 132/280 页

**目的**：让书房**每日打开**。跟现有习惯打卡共享设计语言（连续天数、金币、宝箱）。

---

### 故事 B · "这句话太戳，我寄给 3 个月后的自己"
1. 用户读完《被讨厌的勇气》，添加摘抄：*"重要的不是被给予了什么，而是如何使用被给予的东西"*
2. 点摘抄右侧 **💌 时光胶囊**
3. 选 "3 个月后投递" / "1 年后" / "心情低谷时" / "下个春天"
4. 3 个月后某个早晨，通知响起：
   > 📬 来自 2025-08-27 的你
   > "重要的不是被给予了什么..."
   > —— 你那时读《被讨厌的勇气》时记下的

**目的**：把书房和时光信箱**情感线打通**，制造"被过去的自己温暖到"的瞬间，**唯一性**远超豆瓣。

---

### 故事 C · "深夜想和人聊《三体》"
1. VIP3 用户读完《三体》，进入书页，点 **🤖 和 AI 聊聊这本书**
2. AI 自动加载这本书的人格 prompt（"你是一位读过几千本科幻小说的朋友"）+ 用户的全部摘抄/心得作为上下文
3. 多轮深聊："我觉得罗辑很懦弱" → AI 反问："你觉得他放弃执剑人那一刻，是懦弱还是温柔？" → ...
4. 对话可保存为「读书档案」，按书归档查阅

**目的**：把 AI 从"工具"变成"伙伴"，**让 VIP3 价值具象化**。每月限 5/10/无限条多轮对话（按 VIP 等级）。

---

## 四、功能矩阵 × VIP 解锁阶梯

| 功能 | 普通 | VIP1 月卡 | VIP2 年卡 | VIP3 / 终身 |
|---|:---:|:---:|:---:|:---:|
| **L1 工具** | | | | |
| 书目记录（CRUD + 评分 + 标签） | ✓ | ✓ | ✓ | ✓ |
| 阅读时长打卡 / 连续天数 / 金币 | ✓ | ✓ | ✓ | ✓ |
| 阅读进度追踪（X/Y 页） | ✓ | ✓ | ✓ | ✓ |
| 月度阅读统计图表 | ✓ | ✓ | ✓ | ✓ |
| **L2 情感** | | | | |
| 摘抄记录 | ✓ | ✓ | ✓ | ✓ |
| 💌 摘抄时光胶囊 | 1 条/月 | 5 条/月 | 20 条/月 | 无限 |
| 投递时机（普通：1 周/月/年；VIP+："心情低谷时" / "下个春天" / 自定义） | 三档 | 全部 | 全部 | 全部 |
| **L3 陪伴** | | | | |
| 🤖 AI 读书伴侣单次对话 | 1 条/天 | 5 条/天 | 20 条/天 | 无限 |
| 🤖 AI 读书伴侣**多轮深聊**（>3 轮） | ❌ | ❌ | ❌ | ✓ |
| 📒 长对话存档为「读书档案」 | ❌ | ❌ | ❌ | ✓ |
| 🪄 年度阅读年鉴（原方案 F） | ❌ | ❌ | ❌ | ✓ |

**关键设计**：
- VIP 不是"解锁功能"而是**"扩额度 + 解锁多轮深度"**，避免普通用户完全用不上
- L3 的多轮深聊是 **VIP3 唯一专属**，制造稀缺感

---

## 五、数据模型变更

只需在现有 `Book` entity 增加 2 个字段 + 新建 2 张表：

```kotlin
// 1. 现有 Book 扩展（v53 迁移）
data class Book(
    // ... 原字段
    val totalPages: Int = 0,         // 🆕 总页数
    val currentPage: Int = 0,        // 🆕 阅读进度
)

// 2. 新建：阅读打卡（reading_sessions）
@Entity
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long?,             // 可空（"自由阅读"打卡）
    val minutes: Int,              // 时长
    val dateYmd: Int,              // 20260527 用于连续天数计算
    val createdAt: Long,
)

// 3. 新建：摘抄（quotes）
@Entity
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val bookId: Long,
    val text: String,              // 摘抄正文
    val page: Int = 0,
    val capsuleDeliveryAt: Long = 0L,  // > 0 表示已绑定时光胶囊
    val capsuleDelivered: Boolean = false,
    val createdAt: Long,
)
```

**时光胶囊投递**：复用 `LetterDeliveryWorker` 的扫描机制（再扫一个 quotes 表，同样的 Worker，0 新代码）。

**AI 读书伴侣**：复用 `chat_ai` 云函数 + 现有 `ChatAiCloudRepository`，区别在 `personaSystem` 用书目信息动态拼装。

---

## 六、UI 主要页面（高保真草图文字版）

### Tab1 · 我在读 ▶️
```
┌─────────────────────────────────────┐
│ 🔥 连续阅读 14 天                   │
│ ⏱ 今天读了：[ + 25 分钟 ] [自由]    │   ← 打卡入口
├─────────────────────────────────────┤
│  📖 活着 · 余华                     │
│      ████████░░░░ 132/280 页 (47%) │
│      最近 3 天读了 1h20min          │
│                                     │
│  📖 被讨厌的勇气                    │
│      ██████░░░░░░░ 96/280 页 (34%) │
│      最近 1 天没读，去翻一下？      │
└─────────────────────────────────────┘
```

### Tab2 · 书架 📚（现有页面优化版）
评分色条、标签 chip、保留原 BookCard，新增进度条与"已读完" toggle。

### Tab3 · 摘抄 💭
```
┌─────────────────────────────────────┐
│  从《被讨厌的勇气》                 │
│  "重要的不是被给予了什么..."        │
│  📅 2026-05-27 · 第 87 页           │
│  [💌 寄给未来的我]  [🗂 全部摘抄]    │
└─────────────────────────────────────┘
```

### Tab4 · 数据 📊
- 本月阅读时长曲线
- 累计读完本数 / 平均评分 / Top 标签
- 年鉴入口（VIP3）

### 进入单本书详情
```
[封面色条] 活着
余华 · ★★★★★ · 280 页

📊 你的阅读
   132/280 页 · 已读 5h20min · 始于 5/12

📝 心得 (3 条)              [+ 写心得]
   …

💭 摘抄 (7 条)              [+ 摘一句]
   "..." 💌 已寄给 2026-11-27 的你

🤖 和 AI 聊聊这本书 (VIP3)   ✨ 多轮深聊
   [上次对话 5/24 12 轮] [新对话]
```

---

## 七、复用关系图（0 新增基础设施）

```
┌── 阅光书房 ──────────────────────────────────────────────┐
│                                                          │
│  打卡 ─────────────→ HabitDao + CoinDao（已有）          │
│                                                          │
│  时光胶囊 ─────────→ LetterDeliveryWorker（已有，加扫描） │
│                      Notifications 通道（已有）          │
│                                                          │
│  AI 读书伴侣 ──────→ chat_ai 云函数（已有，加 mode=book）│
│                      ChatAiCloudRepository（已有）       │
│                                                          │
│  年鉴 ─────────────→ chat_ai 云函数（已有）              │
│                                                          │
│  VIP 配额 ─────────→ VipQuota.kt（加 3 个限额方法）      │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

**全部新代码 ≈ 1 个 Worker 扩展 + 2 张表 DAO + 4 个 Screen + 3 个 VipQuota 方法 ≈ 2 天工作量。**

---

## 八、实施路线（建议）

### 🚀 阶段 1（MVP · 1 天）—— 让书房活起来
1. 在 `VipQuota` 加 `readingCapsuleMonthlyLimit` / `aiBookChatDailyLimit` / `aiBookDeepChatUnlocked`
2. 新建 `ReadingSession` entity + DAO + Migration
3. 现有 `BookshelfScreen` 顶部加 **"⏱ 今天读了 X 分钟"** 打卡卡片
4. 连续天数计算 + 金币发放（复用 CoinDao）

### 🚀 阶段 2（情感层 · 1 天）—— 时光胶囊
5. 新建 `Quote` entity + DAO + Migration
6. 在 Book 详情页加 "💭 摘抄" 区块 + "💌 寄给未来的自己"
7. 扩 `LetterDeliveryWorker` 加 quotes 扫描
8. 投递时通过现有通知通道弹出（"📬 来自过去的你"）

### 🚀 阶段 3（陪伴层 · 1 天）—— AI 读书伴侣
9. 在 `BookshelfScreen` 书页底部加 "🤖 和 AI 聊聊这本书"
10. 新建 `BookChatScreen`（复用 ChatBill 风格的对话 UI）
11. `chat_ai` 云函数加 `mode=book`，服务端把书目元数据 + 摘抄拼到 personaSystem
12. VIP3 才能 >3 轮对话（云函数加判断）

### 🚀 阶段 4（润色 · 半天）—— 数据 + 多 Tab
13. 单页改 4-Tab 结构（我在读 / 书架 / 摘抄 / 数据）
14. 加月度阅读时长曲线（用 Compose 自绘简版）

---

## 九、商业化模型估算

| 用户类型 | 用 L1 | 用 L2 | 用 L3 | 付费动机 |
|---|---|---|---|---|
| 重度阅读爱好者（10%） | ✓ | ✓ | ✓ | 想要无限胶囊 + AI 深聊 → **VIP3 主力** |
| 普通爱书人（40%） | ✓ | ✓ | 偶尔 | VIP1/2 月卡：胶囊额度上限 |
| 偶尔读读（50%） | ✓（打卡习惯） | × | × | 不付费，但**贡献 DAU** |

**关键洞察**：L1 打卡保 DAU，L2 胶囊保 7 日留存（用户会回来看胶囊有没有到期），L3 AI 伴侣转化 VIP3。三层各司其职。

---

## 十、风险与权衡

| 风险 | 缓解 |
|---|---|
| 摘抄数据敏感 | 复用现有 `LetterCrypto` 加密存储 |
| AI 调用成本 | chat_ai 服务端已有日额度，按 vipLevel 限流；BookChat 每月限额 |
| 胶囊到期推送被用户关通知 | App 内消息中心二次承接（已有 Inbox） |
| 用户记书但不读 → L1 失效 | 阅读打卡可"自由阅读"不绑书，降门槛 |

---

## 决策点（已确认 ✅）

> 选定方案 **2 + 全部 v53.1 增强**：完整三阶段 + 八大增量 + 晨光胶囊视觉风格。

---

# 🌅 v53.1 增强章节 —— 让书房从"工具"变"情感容器"

## 十一、增量特性总览

| 编号 | 特性 | 价值 | 复用 |
|---|---|---|---|
| E1 | 🪞 读后心境快照 | 仪式感、社交分享 | Compose Canvas |
| E2 | 💌 双向胶囊（开篇期待信 ↔ 读完心境） | 穿越对话、情感共振 | Quote 表加字段 |
| E3 | 🌧 心情低谷召回 | 情绪急救、强情感锚 | MoodDao + Worker |
| E4 | 🌌 匿名摘抄星河 | 共鸣、零社交压力 | 后端 quote_galaxy 集合 |
| E5 | 📮 明信片漂流（VIP2+） | 稀缺惊喜、付费钩子 | LetterDeliveryWorker + 云函数匹配 |
| E6 | 🧬 读者 DNA 人格画像 | 自我认知、强分享 | chat_ai mode=reader_dna |
| E7 | 📈 阅读心电图 | 资产可视化 | ReadingSession 聚合 |
| E8 | 🌅 晨光信使（每日推送） | DAU 引擎 | WorkManager |

---

## 十二、E1 · 读后心境快照（Mood Snapshot）

读完打卡 → 弹出竖版分享卡：
- **构图**：顶部书封软阴影 → 中部"读完此刻"金句（用户写）→ 心情 emoji + 日期邮戳 → 底部 funlife 极简水印
- **色调**：晨光渐变（米白→雾蓝→暖橙），半透明纸纹叠加
- **生成**：纯 Compose `Canvas` + `drawIntoCanvas`，导出 PNG 到 `cacheDir/snapshots/`，FileProvider 分享
- **数据**：复用 `Book.note` + 新增 `Book.finishedMood: String`
- **隔离**：`userId` 强校验，分享图不嵌任何用户标识

## 十三、E2 · 双向胶囊（Opening Letter ↔ Closing Snapshot）

开始读时 `Book.openingLetter` 写一句"我希望从此书获得"。读完那天 App 自动**并排呈现**：
- 左卡：浅米色，标题"开篇的我"，写着开篇期待
- 右卡：晨橙色，标题"读完的我"，心境快照
- **触发**：`Book.finishedAt` 由 0 → >0 时，由 ViewModel 弹"穿越对话"对话框
- **新字段**：`Book.openingLetter: String`、`Book.openingMood: String`

## 十四、E3 · 心情低谷召回（Quiet Rescue）

`LetterDeliveryWorker` 每日扫描时增加分支：
- 查 `mood_entries` 近 3 天均值 ≤ 阈值（负向）
- 若该用户本月未触发过救援 → 从其 `quotes` 中（rating ≥ 4 / `pinned=true`）随机抽 1 条
- 推送 `FunChannel.LETTER`：标题"📬 来自过去的你"
- **不消耗胶囊额度**，写入 `system_quota_used` 表（每月 1 次）
- **隔离**：仅推送给当前 userId 自己产出的 quote

## 十五、E4 · 匿名摘抄星河（Quote Galaxy）

- **发布**：`POST /quote_galaxy/publish` 云函数：HMAC 校验、去除 userId、只存 `text/bookTitle/publishedAt/lightCount`
- **拉取**：`GET /quote_galaxy/feed?cursor=xxx`，每次返 30 颗，前端用 `Canvas` 撒成星空（fbm 噪声铺位置 + 闪烁动画）
- **互动**：仅"接住⭐"（轻量点亮，匿名计数）和"想给我一份"（一键复制为本人 quote）
- **风控**：
  - 客户端发布前 `LetterCrypto` 不参与（明文上行，但服务端审核）
  - 服务端：长度限制 200 字，频控 1 条/天，违禁词字典过滤
  - 每条 quote 带 `reportCount`，达阈值自动隐藏
- **VIP 门槛**：发布 VIP1+，浏览全员
- **后端新集合**：`quote_galaxy { text, bookTitle, publishedAt, lightCount, reportCount, hidden }`

## 十六、E5 · 明信片漂流（Postcard Drift · VIP2+）

- **发件**：用户选 1 条 quote → `letter_ai` 云函数加 `mode=postcard_drift`
- **匹配**：服务端从最近 7 天活跃 VIP2+ 用户中随机选一位（排除自己），在 `postcards` 集合写入 `{fromHash, toUserId, quote, bookTitle, sentAt, reactedHeart}`
- **收件**：被选中用户在 `LetterDeliveryWorker` 下次扫描收到通知"📮 一张从星海漂来的明信片"
- **回应**：仅可点❤（写回 `reactedHeart=true`），双方都看到累计接住数
- **配额**：VIP2 = 1 张/月，VIP3 = 4 张/月
- **隔离**：双方仅暴露 `bookTitle + quote + 城市首字母`，**不暴露 userId/昵称**

## 十七、E6 · 读者 DNA 人格画像（Reader DNA）

- **触发**：每读完 5 本 / 用户主动点"生成画像"
- **服务端**：`chat_ai` 加 `mode=reader_dna`，输入近 N 本书的 title + 摘抄前 200 字 + 评分
- **输出 JSON**：`{rationality, sensibility, inward, outward, gentleness, sharpness, tagline, top_keywords[5]}`
- **UI**：晨光雷达图（Compose 自绘）+ 生成时间戳 + 一键存图
- **本地表**：`reader_dna_cards { id, userId, generatedAt, vector, tagline }`
- **配额**：普通=每年 1 次 / VIP1=每季 1 次 / VIP2=每月 1 次 / VIP3=随时

## 十八、E7 · 阅读心电图（Reading ECG）

- 单本书页底部一条横向波形：x 轴页码、y 轴聚合（停留时长×0.5 + 摘抄密度×0.5）
- **数据来源**：`reading_sessions WHERE bookId=:id` + `quotes WHERE bookId=:id` 实时聚合
- **0 新表**，纯 Compose `Path` 绘制，平滑曲线 + 高峰打点
- **价值密度**：把"读过"沉淀为可视化资产

## 十九、E8 · 晨光信使（Morning Herald）

- **WorkManager** 周期任务，每天 7:30 ± 5 分钟随机触发（避免通知潮）
- **内容池**（按权重抽 1）：
  - 40%：昨日新增 quote
  - 30%：到期或即将到期的胶囊预告
  - 20%：心情近况匹配的"配方书"金句（AI 服务端生成）
  - 10%：星河中"今日热星"
- **频率**：普通 2 次/周；VIP1+ 每天
- **通知**：`FunChannel.LETTER` BigPicture 样式，锁屏可读
- **新表**：`morning_herald_log { userId, dateYmd, contentType }` 防当日重复

---

## 二十、数据模型 v53 完整清单（含增强）

```kotlin
// Book 扩展（v52→v53 迁移加列）
ALTER TABLE books ADD COLUMN totalPages INTEGER NOT NULL DEFAULT 0
ALTER TABLE books ADD COLUMN currentPage INTEGER NOT NULL DEFAULT 0
ALTER TABLE books ADD COLUMN openingLetter TEXT NOT NULL DEFAULT ''
ALTER TABLE books ADD COLUMN openingMood TEXT NOT NULL DEFAULT ''
ALTER TABLE books ADD COLUMN finishedMood TEXT NOT NULL DEFAULT ''

// 新增表
reading_sessions(id, userId, bookId?, minutes, dateYmd, createdAt)
  INDEX(userId), INDEX(userId, dateYmd), INDEX(userId, bookId)

quotes(id, userId, bookId, text, page, rating, pinned, capsuleDeliveryAt,
       capsuleDelivered, publishedToGalaxy, createdAt)
  INDEX(userId), INDEX(userId, bookId), INDEX(userId, capsuleDeliveryAt)

reader_dna_cards(id, userId, generatedAt, vectorJson, tagline)
  INDEX(userId, generatedAt)

morning_herald_log(userId, dateYmd, contentType, sentAt)
  PRIMARY KEY(userId, dateYmd)

system_quota_used(userId, quotaKey, monthYm, count)
  PRIMARY KEY(userId, quotaKey, monthYm)
```

**所有表强制 `userId` 索引；DAO 层所有查询/更新带 `WHERE userId = :userId`。**

---

## 二十一、VipQuota.kt 增量方法

```kotlin
// 阅读时光胶囊月度
fun readingCapsuleMonthlyLimit(vipLevel: Int): Int
// AI 读书伴侣日额度
fun aiBookChatDailyLimit(vipLevel: Int): Int
// AI 读书伴侣是否解锁多轮深聊（>3 轮）
fun aiBookDeepChatUnlocked(vipLevel: Int): Boolean
// 星河发布权限
fun galaxyPublishUnlocked(vipLevel: Int): Boolean
// 明信片漂流月度（VIP2 起）
fun postcardDriftMonthlyLimit(vipLevel: Int): Int
// 读者 DNA 生成间隔
fun readerDnaCooldownDays(vipLevel: Int): Int
// 晨光信使每周次数
fun heraldWeeklyLimit(vipLevel: Int): Int
```

---

## 二十二、开发顺序（已确认执行）

1. **Step 1** · `VipQuota` 全量增量方法
2. **Step 2** · 数据模型 + DAO（Book 扩展 / ReadingSession / Quote / ReaderDnaCard / MorningHeraldLog / SystemQuotaUsed）
3. **Step 3** · `AppDatabase` v52→v53 迁移
4. **Step 4** · Repository 层（ReadingRepository / QuoteRepository / GalaxyRepository / DnaRepository）
5. **Step 5** · 后端云函数：
   - `chat_ai` 加 `mode=book` / `mode=reader_dna`
   - 新建 `quote_galaxy` 函数（publish / feed / report / light）
   - 新建 `postcard_drift` 函数
6. **Step 6** · `LetterDeliveryWorker` 多任务扫描（quotes 胶囊 / 心情召回 / postcard）
7. **Step 7** · `MorningHeraldWorker` 新建
8. **Step 8** · UI：4-Tab `ReadingRoomScreen`（晨光胶囊视觉）
9. **Step 9** · `BookDetailScreen`（开篇期待 + ECG + AI 入口）
10. **Step 10** · `BookChatScreen`、`QuoteGalaxyScreen`、`ReaderDnaScreen`、`SnapshotShareDialog`
11. **Step 11** · `NavGraph` / `HomeScreen` / `MainActivity` 隐藏底栏接入
12. **Step 12** · e2e 测试 + 真机验收清单

每步完成后我会跟你同步进度。
