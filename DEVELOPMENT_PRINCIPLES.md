# FunLife 后续开发原则

> 本文沉淀于 2026-05 完成的两轮全面安全审计（共修复 17 项数据隔离/安全/崩溃问题）。
> **每写一行新代码前，先扫一遍本清单。** 复用规则比写新规则更重要。

---

## 一、多用户数据隔离（最高优先级）

> 项目是单 App 多账号 → **任何"当前用户"信息都必须显式传递，不允许默认值兜底**。

### 1.1 DAO / Entity 设计
- **所有 `@Query` 必须包含 `WHERE userId = :userId`**，除非是真正的"全局商品/字典表"（且禁止写入用户私有字段）。
- **DAO 方法的 `userId` 参数禁止写默认值**：
  ```kotlin
  // ❌ 错误：调用方忘传时静默读 user 1 数据
  fun getAllItems(userId: Long = 1): Flow<List<InventoryItem>>

  // ✅ 正确：编译期强制传入
  fun getAllItems(userId: Long): Flow<List<InventoryItem>>
  ```
- **Entity 的 `userId` 字段也不要给默认值**，避免 `Item(...)` 构造时漏传被悄悄归到 `userId = 1`。
- 禁止编写 `SELECT * FROM xxx`（无 userId 过滤）的 `@Query`，**包括"获取所有 List 用于迁移/统计"的辅助方法**——一律加 `userId`。

### 1.2 Repository / ViewModel
- Repository 的对外方法签名同样**禁止 `userId: Long = 1` 默认值**。
- ViewModel 必须通过 **构造参数** 接收 `currentUserId: Long`，而不是在 `init {}` 里调 `sessionManager.getCurrentUserId()` 后冻结进 StateFlow。
  - 否则登出/换号后 ViewModel 复用，StateFlow 仍按旧 userId 查询。
- 凡涉及 userId 的 StateFlow，要么走构造参数，要么走 `MutableStateFlow<Long>` + `flatMapLatest`。
- **绝对禁止在业务代码里写 `userId = 1L` / `userId = 0L` 字面量**。需要"当前用户"时：
  - ViewModel 内：用构造参数 `currentUserId`
  - Composable 内：从 `authViewModel.getCurrentSession()?.userId` 或 `UserSessionManager(ctx).getCurrentUserId()` 读取并 `remember(currentUserId) { ... }`

### 1.3 NavGraph 实例化 ViewModel
- 任何依赖 userId 的 ViewModel，在 NavGraph 中实例化时必须给 `viewModel(...)` / `remember(...)` 加 **userId 作为 key**：
  ```kotlin
  val vm = viewModel(
      key = "inventory_${userSession.userId}",
      factory = ...
  )
  // 或：
  val vm = remember(userSession.userId) { ChatViewModel(application, userSession.userId) }
  ```
- 未登录访问需要登录的页面 → `LaunchedEffect` 跳到 Login，而不是用 `userId = -1` 兜底加载空数据。

### 1.4 退出登录 / 切换账号
- **登出回调里必须 `(context as Activity).recreate()`**，彻底销毁所有 Activity 范围 ViewModel 的 `ViewModelStore`。
- 仅 `navController.popUpTo(0)` 不够，因为 `viewModel()` 默认绑定到 Activity 而非 NavBackStackEntry。

### 1.5 SharedPreferences
- **任何用户态数据进入 SharedPreferences 前必须按 userId 命名 key**：
  - 推荐前缀法：`"persona_avatar_${userId}_$personaId"`、`"u${userId}_$key"`
- 跨日重置/批量清理时，**不要 `editor.clear()` 整个文件**，只删该用户的 key（避免误删其他账号数据）。
- 全局表无法加 `userId` 列时（如 `chat_personas`），通过 SharedPreferences 维护 **per-user 覆盖层**，在 ViewModel 的 Flow 上 `combine` 叠加，**不要直接修改全局表**。

### 1.6 文件存储
- 用户私有文件命名必须包含 userId，例如 `avatar_${userId}_${uuid}.jpg`。
- 清理用户数据时按 `startsWith("xxx_${userId}_")` 严格筛选，不要 `deleteRecursively()` 整个目录。
- `FileProvider` 暴露目录时只暴露最小路径（`<cache-path>` 或专用子目录），不要 `<external-path path="."/>` 全暴露。

---

## 二、安全敏感数据

### 2.1 密码 / Token / API Key
- 密码统一走 `PasswordHasher`（PBKDF2 + 随机 salt + 常量时间比较），**禁止明文 / MD5 / 单 SHA256 存库**。
- API Key、敏感 token 必须用 `EncryptedSharedPreferences`（带 `try/catch` 回退到普通 prefs，避免极端机型 Keystore 异常导致崩溃）。
- API Key 也要按 userId 命名空间隔离（`ai_api_key_<userId>`），不同账号互不影响。

### 2.2 日志
- **生产环境（`!BuildConfig.DEBUG`）任何 OkHttp Logging 必须为 `Level.NONE`**，DEBUG 也最多 `Level.BASIC`。
- `redactHeader("Authorization")`、`redactHeader("Cookie")` 双保险。
- **永远不要 `Log.d("...", "key=${apiKey.take(8)}")`** 这类"截断脱敏"，截断也是泄漏。
- 不要打印密码、token、原始请求体、用户手机号/邮箱等。

### 2.3 登录错误信息
- 用户名不存在 / 密码错误 **必须合并为统一提示**（如"用户名或密码错误"），防止用户名枚举攻击。

### 2.4 PendingIntent
- 一律 `PendingIntent.FLAG_IMMUTABLE`（API 23+）。

---

## 三、崩溃防御

### 3.1 几何/数学
- Compose Canvas 中 `radius`、`size`、`stroke width` **必须 `coerceAtLeast(1f)` 或在使用前判 `> 0f`**，否则渲染 NaN/Infinity 会闪退（VIP3 SupernovaFlash 教训）。
- 除法前判分母：`if (total > 0) value / total else 0`，对动画/转盘 `360f / list.size` 要 `if (list.isEmpty()) return`。
- `bitmap.createScaledBitmap(_, w, h, _)` 中 `w/h` 必须 `coerceAtLeast(1)`。

### 3.2 集合
- 不能保证非空的集合：`firstOrNull()` / `getOrNull(i)` / `maxByOrNull` 而非 `first()` / `[i]` / `maxBy`。
- `random()` 仅对硬编码非空 `listOf(a, b, c)` 直接调用；动态来源先判空。

### 3.3 Flow
- `.first()` / `.last()` 在协程里若 Flow 永不发射会卡死，配合 `withTimeout(...)` 或确保 Flow 一定有初值。
- StateFlow 初始化时给合理 `initialValue`，UI 避免 `!!`。

### 3.4 异步 / 并发
- ViewModel 协程里访问 DB 必须 `try/catch`，单条 SQL 抛出别让 ViewModelScope 整个挂掉。
- `withContext(Dispatchers.IO)` 做 IO，主线程不要做加解密/JSON 大对象解析。

---

## 四、UI / 动画

- 所有 `LaunchedEffect(key)` 的 key 要正确：和"重新触发"的依赖完全一致，否则要么不触发要么疯狂触发。
- 转盘/抽奖类组件：**先确定结果，再算"指针停在该结果中心"的精确角度**，不要先随机角度再倒推结果，否则前端结果与后端发奖会不一致。
- 同一帧内不要既改 `targetValue` 又重置 `Animatable`，会出现跳变；用 `LaunchedEffect(triggerKey)` 串行化。

---

## 五、Room 数据库迁移

- 任何 `Entity` 的字段增删 / 默认值变化 / 类型变化都必须**写 `Migration`** 并把版本号 +1。
  - 注意：去掉 Kotlin 默认值（如把 `userId: Long = 1` 改为 `userId: Long`）**不需要**迁移（不影响 SQL schema），但要小心调用方编译期是否还能通过。
- 新建表统一加索引：`@Index("userId")` 等高频过滤字段。
- 禁止 `fallbackToDestructiveMigration()` 上线。

---

## 六、代码评审 Checklist（提交前自查）

新功能 PR 前，逐条勾选：

- [ ] 新增 / 修改的 DAO 方法都带 `userId` 参数且**无默认值**
- [ ] 新增 Entity 的 `userId` 字段**无默认值**
- [ ] 业务代码里没有 `userId = 1L` / `userId = 0L` 字面量
- [ ] 新增 ViewModel 通过构造参数拿 `currentUserId`，而非 `init` 内冻结
- [ ] NavGraph 中实例化按 userId 加 `key = ...`
- [ ] 新增 SharedPreferences key 含 `userId`
- [ ] 新增写文件路径含 `userId`
- [ ] 新增网络日志 `Level` 与 `redactHeader` 配置正确
- [ ] 新增动画/绘制 `radius`、`size`、`分母` 已 `coerceAtLeast(1f)` 或非零判断
- [ ] 新增 Room 字段已写 `Migration`、版本号 +1
- [ ] 新增日志没有打印 password / token / apiKey / 长 body
- [ ] 编译通过：`.\gradlew.bat :app:compileDebugKotlin -q`

---

## 七、典型反例（来自历史 bug）

| 反例 | 后果 |
|---|---|
| `fun getAllItems(userId: Long = 1)` | 调用方忘传 → 全员看到 user 1 背包 |
| `InventoryItem(userId = 1L, ...)` 写死 | 任何用户购买都挂到 user 1 |
| `init { repository.get(getCurrentUserId()) }` 进 StateFlow | 切换账号后 StateFlow 还在查旧用户 |
| `chat_personas` 无 userId 列，直接 `UPDATE … customAvatar` | 用户 A 改头像，所有账号都变 |
| `getSharedPreferences("pet_mission").edit().clear()` 跨日重置 | 把所有账号的进度都清掉 |
| `prefs.getString("ai_api_key", ...)` 明文 + `Level.BODY` | 日志泄漏 + 跨账号共用同一 key |
| `radius = animValue` 但起始 `0f` | Canvas 闪退 |
| `360f / options.size` 不判空 | NaN / 触发 IllegalArgument |
| `if (existingUser == null) "用户名不存在"` | 用户名枚举攻击 |
| 登出仅 `popUpTo(0)`，不 `recreate()` | Activity 范围 ViewModel 仍持有旧 userId 数据 |

---

**最后一句话原则**：
> *"如果你不能立刻指出一段代码里的 userId 是从哪个登录会话流过来的，那段代码就有问题。"*
