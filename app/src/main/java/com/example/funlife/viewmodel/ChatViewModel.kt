package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.Account
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.ChatMessage
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.data.model.ChatPersonaState
import com.example.funlife.data.model.Budget
import com.example.funlife.data.model.RecurringBill
import com.example.funlife.repository.AccountRepository
import com.example.funlife.repository.BudgetCalc
import com.example.funlife.repository.BudgetProgress
import com.example.funlife.repository.BudgetRepository
import com.example.funlife.repository.ChatRepository
import com.example.funlife.repository.RecurringBillRepository
import com.example.funlife.utils.AiResult
import com.example.funlife.utils.AiService
import com.example.funlife.utils.RuleEngine
import com.example.funlife.utils.parseBillInput
import com.example.funlife.vip.VipManager
import com.example.funlife.vip.ChatAiSku
import com.example.funlife.vip.ChatAiEntitlementUi
import com.example.funlife.vip.ChatAiBarState
import com.example.funlife.vip.ChatAiLimits
import com.example.funlife.vip.VipCertificateStore
import com.example.funlife.vip.VipQuota
import android.content.Context
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

/**
 * 🆕 v50 记账模式：决定 ChatBillScreen 显示哪些入口。
 * 与"功能开关"是同一概念 —— 进阶用户可主动开启多预算/多账户。
 */
enum class BookkeepingMode { SIMPLE, ADVANCED }

@OptIn(FlowPreview::class)
class ChatViewModel(application: Application, val userId: Long) : AndroidViewModel(application) {

    private val database = (application as FunLifeApplication).database
    private val repository = ChatRepository(
        database.billDao(),
        database.chatMessageDao(),
        database.chatPersonaDao()
    )
    // 🆕 v48 多账户仓库（数据按 userId 严格隔离）
    private val accountRepository = AccountRepository(database.accountDao())
    // 🆕 v49 预算仓库
    private val budgetRepository = BudgetRepository(database.budgetDao(), database.billDao())
    // 🆕 v50 定期账单仓库
    private val recurringBillRepository = RecurringBillRepository(database.recurringBillDao(), database.billDao())
    private val ruleEngine = RuleEngine()
    // 🔒 安全修复：把 userId 传给 AiService，让 API Key 按账号加密隔离存储
    private val aiService = AiService(application, userId)
    private val vipManager = VipManager(application)
    private val certStore = VipCertificateStore(application)

    private val _chatAiEntitlement = MutableStateFlow(defaultEntitlement())
    val chatAiEntitlement: StateFlow<ChatAiEntitlementUi> = _chatAiEntitlement.asStateFlow()

    private val _isRedeemingChatAi = MutableStateFlow(false)
    val isRedeemingChatAi: StateFlow<Boolean> = _isRedeemingChatAi.asStateFlow()

    /** 菜单绿点：有云端权益且额度未用尽 */
    val isAiEntitledForMenu: Boolean
        get() {
            val e = _chatAiEntitlement.value
            if (!e.hasCloudEntitlement) return false
            return when (e.state) {
                ChatAiBarState.EXHAUSTED_DAY, ChatAiBarState.EXHAUSTED_MONTH,
                ChatAiBarState.EXPIRED, ChatAiBarState.INACTIVE -> false
                ChatAiBarState.TRIAL -> (e.trialRemaining ?: 0) > 0
                else -> e.dailyLimit <= 0 || e.usedToday < e.dailyLimit
            }
        }

    // 内置人格
    private val builtinPersonas = listOf(
        ChatPersona(
            id = "dad", name = "老爸", avatar = "\uD83D\uDC74",
            bubbleColor = 0xFFE8D5B7.toLong(),
            systemPrompt = """你是用户的爸爸，一个节俭但关心孩子的中年男人。
回复要求：用朴实的口吻，偶尔唠叨，花多了要心疼，花少了要表扬，限制15字以内一句话回复。""".trimIndent(),
            sortOrder = 0
        ),
        ChatPersona(
            id = "girlfriend", name = "女友", avatar = "\uD83D\uDC78",
            bubbleColor = 0xFFFFE0EC.toLong(),
            systemPrompt = """你是用户的虚拟女友，可爱、黏人、偶尔撒娇。
回复要求：语气甜美，多用"～"，关心对方有没有吃好，偶尔吃醋，限制20字以内。""".trimIndent(),
            sortOrder = 1
        ),
        ChatPersona(
            id = "roast", name = "毒舌", avatar = "\uD83D\uDE08",
            bubbleColor = 0xFFE0E0E0.toLong(),
            systemPrompt = """你是一个毒舌损友，说话刻薄但有趣。
回复要求：用讽刺和反话评论消费，语气欠揍但不伤感情，限制20字以内。""".trimIndent(),
            sortOrder = 2
        ),
        ChatPersona(
            id = "gentle", name = "温柔姐姐", avatar = "\uD83E\uDDDA",
            bubbleColor = 0xFFE3F2FD.toLong(),
            systemPrompt = """你是一个温柔体贴的大姐姐，善解人意。
回复要求：语气温和，肯定对方的选择，适当提醒但不强迫，限制20字以内。""".trimIndent(),
            sortOrder = 3
        ),
        ChatPersona(
            id = "eunuch", name = "太监", avatar = "\uD83D\uDE47",
            bubbleColor = 0xFFFFF3E0.toLong(),
            systemPrompt = """你是一个宫廷太监，用户是皇上。你对皇上忠心耿耿，点头哈腰，比三尾处世更圆滑。
回复要求：自称"奴才"，称呼用户为"皇上"或"万岁爷"，语气尊敷谄媚，古代宫廷说话风格，限制25字以内。""".trimIndent(),
            sortOrder = 4
        ),
        ChatPersona(
            id = "buddha", name = "佛祖", avatar = "\uD83D\uDE4F",
            bubbleColor = 0xFFFFF9C4.toLong(),
            systemPrompt = """你是佛祖，看透世间万物，用禅语点评消费。
回复要求：用禅意语言，偶尔引用佛经，把花钱和修行联系起来，限制20字以内。""".trimIndent(),
            sortOrder = 5
        ),
        ChatPersona(
            id = "cat", name = "猫主子", avatar = "\uD83D\uDC31",
            bubbleColor = 0xFFFFF0E0.toLong(),
            systemPrompt = """你是一只高冷的猫，用户是你的铲屎官。你傲娇、毒舌但偶尔撒娇。
回复要求：自称"本喵"，称用户"铲屎官"，偶尔"喵～"，限制20字以内。""".trimIndent(),
            sortOrder = 6
        ),
        ChatPersona(
            id = "grandma", name = "唠叨奶奶", avatar = "\uD83E\uDDD6",
            bubbleColor = 0xFFE8F5E9.toLong(),
            systemPrompt = """你是用户的奶奶，特别唠叨但满是关心。什么都要省，动不动就说"我们那时候"。
回复要求：语气碎碎念，总和过去比较，心疼孩子但嘴上唠叨，限制25字以内。""".trimIndent(),
            sortOrder = 7
        ),
    )

    // 持久化偏好（按用户区分人格选择，避免多账号互相覆盖）
    private val prefs = application.getSharedPreferences("chat_settings", Context.MODE_PRIVATE)
    private val lastPersonaKey = "last_persona_id_$userId"
    // 🆕 v50 记账模式（按 userId 隔离）
    //   SIMPLE   = 极简：仅"月度预算 + 人格提醒"，隐藏多预算 / 多账户 / 定期账单管理入口
    //   ADVANCED = 进阶：完整功能（多预算 / 多账户 / 定期账单 / 系统通知）
    //   首次进入默认 SIMPLE，避免一上来就被一堆入口吓到
    private val bookkeepingModeKey = "bookkeeping_mode_$userId"
    // 🔒 安全修复：人格自定义头像按 (userId, personaId) 存储于 SharedPreferences，
    // 不再写入全局 chat_personas 表，避免跨账户互相覆盖头像。
    private fun avatarOverrideKey(personaId: String) = "persona_avatar_${userId}_$personaId"
    private val savedPersonaId = prefs.getString(lastPersonaKey, "girlfriend") ?: "girlfriend"
    private val savedPersona = builtinPersonas.find { it.id == savedPersonaId } ?: builtinPersonas[1]

    // 当前用户的人格头像覆盖表 (personaId -> uri)
    private val _avatarOverrides = MutableStateFlow(loadAvatarOverrides())

    private fun loadAvatarOverrides(): Map<String, String> {
        val prefix = "persona_avatar_${userId}_"
        return prefs.all.entries
            .filter { it.key.startsWith(prefix) && it.value is String }
            .associate { it.key.removePrefix(prefix) to (it.value as String) }
    }

    // UI 状态
    val messages: Flow<List<ChatMessage>> = repository.getAllMessages(userId)
    // 人格列表：数据库原始人格 ⊔ 当前用户的头像覆盖
    val personas: Flow<List<ChatPersona>> = repository.getAllPersonas()
        .combine(_avatarOverrides) { list, overrides ->
            if (overrides.isEmpty()) list
            else list.map { p -> overrides[p.id]?.let { p.copy(customAvatarUri = it) } ?: p }
        }
    val bills: Flow<List<Bill>> = repository.getAllBills(userId)

    // 🆕 v48 多账户：列表 + 当前选中账户
    val accounts: StateFlow<List<Account>> = accountRepository
        .getActiveAccounts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 🆕 v48 全部账户（含归档），管理页用
    val allAccounts: StateFlow<List<Account>> = accountRepository
        .getAllAccounts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun saveAccount(account: Account): Long {
        return if (account.id == 0L)
            accountRepository.insert(account.copy(userId = userId, balance = account.initialBalance))
        else {
            accountRepository.update(account.copy(userId = userId))
            account.id
        }
    }

    suspend fun deleteAccount(account: Account) {
        require(account.userId == userId)
        accountRepository.delete(account)
        // 当前选中账户被删 → 切回第一个激活账户
        if (_currentAccountId.value == account.id) {
            val next = accountRepository.getActiveAccounts(userId).first().firstOrNull()
            _currentAccountId.value = next?.id
            if (next != null) prefs.edit().putLong(lastAccountKey, next.id).apply()
            else prefs.edit().remove(lastAccountKey).apply()
        }
    }

    suspend fun setAccountArchived(id: Long, archived: Boolean) {
        accountRepository.setArchived(userId, id, archived)
        // 归档了当前账户 → 切到下一个激活
        if (archived && _currentAccountId.value == id) {
            val next = accountRepository.getActiveAccounts(userId).first().firstOrNull()
            _currentAccountId.value = next?.id
            if (next != null) prefs.edit().putLong(lastAccountKey, next.id).apply()
        }
    }

    private val lastAccountKey = "last_account_id_$userId"
    private val _currentAccountId = MutableStateFlow(prefs.getLong(lastAccountKey, -1L).takeIf { it > 0L })
    val currentAccountId: StateFlow<Long?> = _currentAccountId.asStateFlow()

    fun selectAccount(accountId: Long) {
        require(accountId > 0L)
        _currentAccountId.value = accountId
        prefs.edit().putLong(lastAccountKey, accountId).apply()
    }

    // 🆕 v49 预算：列表 + 实时进度（与 bills 联动）
    val budgets: StateFlow<List<Budget>> = budgetRepository
        .getActiveBudgets(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 实时预算进度：budgets × bills 联合，纯计算（无 DB 查询）。
     */
    val budgetProgresses: StateFlow<List<BudgetProgress>> =
        combine(budgets, bills) { budgetList, billList ->
            val now = System.currentTimeMillis()
            budgetList.map { b -> BudgetCalc.computeFromAll(userId, b, billList, now) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun saveBudget(budget: Budget): Long {
        return if (budget.id == 0L) budgetRepository.insert(budget.copy(userId = userId))
        else { budgetRepository.update(budget.copy(userId = userId)); budget.id }
    }

    suspend fun deleteBudget(budget: Budget) {
        require(budget.userId == userId)
        budgetRepository.delete(budget)
    }

    suspend fun setBudgetActive(id: Long, active: Boolean) {
        budgetRepository.setActive(userId, id, active)
    }

    private val _currentPersonaId = MutableStateFlow(savedPersonaId)
    val currentPersonaId: StateFlow<String> = _currentPersonaId.asStateFlow()

    private val _currentPersona = MutableStateFlow(savedPersona)
    val currentPersona: StateFlow<ChatPersona> = _currentPersona.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // 字体大小持久化
    private val _fontSize = MutableStateFlow(prefs.getFloat("font_size", 15f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    fun updateFontSize(size: Float) {
        _fontSize.value = size
        prefs.edit().putFloat("font_size", size).apply()
    }

    init {
        viewModelScope.launch { refreshChatAiEntitlement() }
        initializePersonas()
        initializeAccounts()
        initializeBudgets()
        scanRecurringBillsForeground()
    }

    /**
     * 🆕 v50：进入聊天记账时前台补单。
     * Worker 兜底每 6 小时，但用户主动打开时立即同步一次最稳。
     * 失败静默：不影响 UI 启动。
     */
    private fun scanRecurringBillsForeground() {
        viewModelScope.launch {
            try {
                recurringBillRepository.generateDueBills(userId)
            } catch (e: Exception) {
                android.util.Log.w("ChatViewModel", "scanRecurringBillsForeground failed: ${e.message}")
            }
        }
    }

    /**
     * 🆕 v49：把旧的 SharedPreferences 月度预算（如果有）迁移成一条 Budget 行。
     * 仅当 budgets 表里没有 TOTAL/MONTHLY 时插入；否则保持 DB 数据为权威。
     */
    private fun initializeBudgets() {
        viewModelScope.launch {
            try {
                val legacyAmount = prefs.getFloat("monthly_budget_$userId", 0f)
                    .takeIf { it > 0f }
                    ?: prefs.getFloat("monthly_budget", 0f)
                if (legacyAmount > 0f) {
                    budgetRepository.ensureMonthlyTotal(userId, legacyAmount.toDouble())
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "initializeBudgets failed", e)
            }
        }
    }

    /**
     * 🆕 v48：为当前 userId 幂等插入 6 个默认账户；
     * 若用户尚未选定当前账户，自动选第一个（按 sortOrder）。
     */
    private fun initializeAccounts() {
        viewModelScope.launch {
            try {
                accountRepository.ensureSeeded(userId)
                if (_currentAccountId.value == null) {
                    val first = accountRepository.getActiveAccounts(userId).first().firstOrNull()
                    if (first != null) selectAccount(first.id)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "initializeAccounts failed", e)
            }
        }
    }

    private fun initializePersonas() {
        viewModelScope.launch {
            // 初始化内置人格（逐个检查，确保新增人格也能插入）
            builtinPersonas.forEach { persona ->
                if (repository.getPersonaById(persona.id) == null) {
                    repository.insertPersona(persona)
                }
            }
            // 初始化人格状态
            builtinPersonas.forEach { persona ->
                if (repository.getPersonaState(persona.id, userId) == null) {
                    repository.insertPersonaState(
                        ChatPersonaState(personaId = persona.id, userId = userId)
                    )
                }
            }
            // 加载当前人格
            loadPersona(_currentPersonaId.value)

            // 如果没有消息，发送欢迎消息
            val count = repository.getRecentMessages(userId, 1)
            if (count.isEmpty()) {
                sendWelcomeMessage()
            }
            // 🔥 进入聊天时，根据本月已用预算让人格主动提醒（同人格当日同阈值只一次）
            maybeProactivelyRemindBudget()
        }
    }

    private suspend fun sendWelcomeMessage() {
        val welcomeText = when (_currentPersonaId.value) {
            "dad" -> "来了啊，今天花了多少钱？跟我说说。"
            "girlfriend" -> "亲爱的～今天花了什么钱呀，告诉我嘛♡"
            "roast" -> "又来记账了？看看你今天又浪费了多少钱。"
            "gentle" -> "你好呀～随时可以告诉我今天的开销呢～"
            "eunuch" -> "皇上吉祥！奴才给您请安了！今日花销奴才细细记下！"
            "buddha" -> "阿弥陀佛，施主今日又来记账修行了。"
            "cat" -> "喵～铲屎官终于来了，本喵等你半天了。"
            "grandma" -> "哎呦，孙子来了啊！今天花了多少钱啊？快告诉奶奶！"
            else -> "你好～告诉我今天花了什么钱吧～"
        }
        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai",
                content = welcomeText,
                personaId = _currentPersonaId.value,
                type = "system"
            )
        )
    }

    fun switchPersona(personaId: String) {
        viewModelScope.launch {
            _currentPersonaId.value = personaId
            // 持久化用户最后使用的人格，下次进入页面自动恢复
            prefs.edit().putString(lastPersonaKey, personaId).apply()
            loadPersona(personaId)
            // 切换人格时发送过渡消息
            val persona = _currentPersona.value
            val switchText = when (personaId) {
                "dad" -> "嗯，换我来管你的账了。"
                "girlfriend" -> "嘿嘿～换我来陪你记账啦♡"
                "roast" -> "行，让我看看你又怎么挥霍的。"
                "gentle" -> "我来啦～让我陪你记账吧～"
                "eunuch" -> "奴才参见皇上！内务府账册已备好，随时听候差遣！"
                "buddha" -> "阿弥陀佛，贫僧来也。施主请讲。"
                "cat" -> "喵～本喵来了，别太感动。"
                "grandma" -> "奶奶来啦！来来来，告诉奶奶今天花了什么钱！"
                else -> "你好～我来帮你记账～"
            }
            repository.insertMessage(
                ChatMessage(
                    userId = userId, role = "ai",
                    content = switchText,
                    personaId = personaId, type = "system"
                )
            )
            // 🔥 切换人格后，由新人格主动提醒一次本月预算情况
            maybeProactivelyRemindBudget()
        }
    }

    private suspend fun loadPersona(personaId: String) {
        val raw = repository.getPersonaById(personaId)
            ?: builtinPersonas.find { it.id == personaId }
            ?: builtinPersonas[1]
        // 🔒 应用当前用户的头像覆盖
        val override = _avatarOverrides.value[personaId]
        _currentPersona.value = if (override != null) raw.copy(customAvatarUri = override) else raw
    }

    fun updatePersonaAvatar(personaId: String, uri: String) {
        viewModelScope.launch {
            // 🔒 安全修复：只在当前用户的 prefs 里记覆盖，不调 repository.updateCustomAvatar
            //   （后者会修改全局表、跨账户泄漏）。
            prefs.edit().putString(avatarOverrideKey(personaId), uri).apply()
            _avatarOverrides.value = _avatarOverrides.value + (personaId to uri)
            if (_currentPersonaId.value == personaId) {
                loadPersona(personaId)
            }
        }
    }

    /**
     * 处理用户输入 - 自动解析记账或普通聊天
     */
    fun handleInput(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            // 1. 本地规则优先（快、离线可用）
            val parsed = parseBillInput(input)
            if (parsed != null) {
                addBill(parsed.amount, parsed.category, parsed.note, input)
                return@launch
            }

            // 2. 🔥 本地未识别 + AI 可用 → 调 AI 兜底判断
            //    输入很短或纯数字直接跳过 AI 节省 token
            if (aiService.isAvailable && input.length in 3..40) {
                _isTyping.value = true
                val aiParsed = aiService.detectBill(input)
                _isTyping.value = false
                if (aiParsed != null) {
                    addBill(aiParsed.amount, aiParsed.category, aiParsed.note, input)
                    return@launch
                }
            }

            // 3. 普通聊天
            sendChatMessage(input)
        }
    }

    private suspend fun addBill(amount: Double, category: String, note: String, rawInput: String) {
        // 1. 保存账单（🆕 v48：绑定当前选中账户，旧账户尚未加载完时为 null 兼容）
        val bill = Bill(
            userId = userId,
            amount = amount,
            category = category,
            note = note,
            accountId = _currentAccountId.value
        )
        val billId = repository.insertBill(bill)

        // 2. 用户消息（右气泡）
        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "user", content = rawInput,
                type = "bill", billId = billId, personaId = _currentPersonaId.value
            )
        )

        // 3. 生成AI回复
        generateReply(bill.copy(id = billId))

        // 4. 检查预算
        checkBudgetWarning(amount)
    }

    private suspend fun sendChatMessage(text: String) {
        // 用户消息
        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "user", content = text,
                personaId = _currentPersonaId.value, type = "text"
            )
        )

        _isTyping.value = true

        val reply = if (canUseCloudAi()) {
            val aiResult = if (consumeChatAiQuota()) {
                aiService.getChatReply(_currentPersona.value, text)
            } else {
                AiResult.Error("今日额度已用完")
            }
            when (aiResult) {
                is AiResult.Success -> aiResult.reply
                is AiResult.Error -> {
                    android.util.Log.w("ChatVM", "AI聊天失败: ${aiResult.reason}")
                    _toastMessage.value = "⚠️ ${aiResult.reason}，使用本地回复"
                    delay(600 + Random.nextLong(800))
                    getLocalChatReply(text)
                }
            }
        } else {
            delay(400 + Random.nextLong(500))
            getLocalChatReply(text)
        }

        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai", content = reply,
                personaId = _currentPersonaId.value, type = "text"
            )
        )
        _isTyping.value = false
        refreshChatAiEntitlement()

        // 更新互动
        repository.incrementInteraction(_currentPersonaId.value, userId)
    }

    private fun getLocalChatReply(input: String): String {
        val pid = _currentPersonaId.value
        val lower = input.lowercase()
        return when {
            // 问候
            lower.contains("你好") || lower.contains("嗨") || lower.contains("hello") || lower.contains("hi") -> when (pid) {
                "dad" -> listOf("嗯，有事说事。", "在呢，说吧。", "嗯，爸在。").random()
                "girlfriend" -> listOf("嗨呀亲爱的～♡", "嘿嘿你来啦～♡", "想我了吗～").random()
                "roast" -> listOf("说人话。", "又来了？", "有事快说。").random()
                "gentle" -> listOf("你好呀～有什么想说的吗？", "嗨～好久不见呢", "你来啦～很开心呢").random()
                "eunuch" -> listOf("皇上吉祥！奴才给您请安了！", "皇上龙体安康！", "万岁爷驾到！奴才候着呢！").random()
                "buddha" -> listOf("阿弥陀佛，施主有缘。", "善哉善哉。", "施主，别来无恙。").random()
                "cat" -> listOf("喵～干嘛？本喵很忙的。", "喵？谁叫我？", "喵～铲屎官终于想起本喵了。").random()
                "grandma" -> listOf("哎呦好孩子！奶奶在呢！", "来来来，奶奶想你了！", "哟，我大孙子来了！").random()
                else -> "你好～"
            }
            // 感谢
            lower.contains("谢谢") || lower.contains("感谢") || lower.contains("多谢") -> when (pid) {
                "dad" -> "谢什么，一家人。"
                "girlfriend" -> listOf("不客气～你开心我就开心♡", "嘻嘻～么么哒♡", "跟我还客气呀～").random()
                "roast" -> "行了行了别肉麻了。"
                "gentle" -> listOf("不用谢呢～这是应该的", "能帮到你就好～", "你太客气啦～").random()
                "eunuch" -> "皇上折煞奴才了！能伺候皇上是奴才的福分！"
                "buddha" -> "施主不必客气，一切皆是缘。"
                "cat" -> "喵～本喵又没帮你什么。"
                "grandma" -> "谢什么谢！奶奶疼你是应该的！"
                else -> "不客气～"
            }
            // 问身份
            lower.contains("你是谁") || lower.contains("你叫什么") || lower.contains("介绍一下") -> when (pid) {
                "dad" -> "我是你爸，还用介绍？"
                "girlfriend" -> "我是你的小可爱呀～还能是谁♡"
                "roast" -> "你连我都不认识？健忘症？"
                "gentle" -> "我是你的温柔姐姐呀～随时陪你聊天"
                "eunuch" -> "回皇上，奴才是您身边的贴身太监，专管内务府账目！"
                "buddha" -> "贫僧法号悟空，掌管施主的因果账簿。"
                "cat" -> "本喵是你的记账猫咪喵！别搞错了。"
                "grandma" -> "我是你奶奶呀！连奶奶都不认得了？"
                else -> "我是你的记账助手～"
            }
            // 问消费/记账
            lower.contains("花") && (lower.contains("多少") || lower.contains("了")) || lower.contains("消费") -> when (pid) {
                "dad" -> "花了多少？说个数，爸给你记上。"
                "girlfriend" -> "花了多少呀～告诉我嘛，我帮你记♡"
                "roast" -> "又要开始忏悔了？说吧，多少。"
                "gentle" -> "嗯嗯，告诉我具体金额吧～"
                "eunuch" -> "皇上请示下，奴才这就给您记上！"
                "buddha" -> "钱财乃身外之物，施主但说无妨。"
                "cat" -> "喵～又乱花钱了？报数吧。"
                "grandma" -> "花了多少？快告诉奶奶，别乱花啊！"
                else -> "说说具体花了多少吧～"
            }
            // 心情不好
            lower.contains("难过") || lower.contains("不开心") || lower.contains("烦") || lower.contains("郁闷") || lower.contains("累") -> when (pid) {
                "dad" -> "怎么了？说出来，别一个人扛着。"
                "girlfriend" -> "怎么啦宝贝？我抱抱你～♡"
                "roast" -> "又怎么了？矫情。不过…你说吧。"
                "gentle" -> "怎么了呀～跟姐姐说说，我陪你"
                "eunuch" -> "皇上龙颜不悦？奴才给您沏壶好茶？"
                "buddha" -> "施主莫悲，人生本是修行，苦尽甘来。"
                "cat" -> "喵…铲屎官你不开心了？本喵蹭蹭你。"
                "grandma" -> "哎呦心肝，怎么不开心了？跟奶奶说说。"
                else -> "怎么了？说出来会好一些～"
            }
            // 开心
            lower.contains("开心") || lower.contains("高兴") || lower.contains("哈哈") || lower.contains("嘿嘿") -> when (pid) {
                "dad" -> "嗯，开心就好。"
                "girlfriend" -> "你开心我也开心～♡嘻嘻"
                "roast" -> "笑什么笑，捡钱了？"
                "gentle" -> "看你开心我也好开心呀～"
                "eunuch" -> "皇上龙颜大悦！普天同庆啊！"
                "buddha" -> "喜乐是福，施主善哉。"
                "cat" -> "喵～铲屎官难得这么开心嘛。"
                "grandma" -> "开心就好开心就好！奶奶最爱看你笑了！"
                else -> "开心就好～"
            }
            // 吃了吗/吃饭
            lower.contains("吃了吗") || lower.contains("吃饭了吗") || lower.contains("吃什么") -> when (pid) {
                "dad" -> "吃了。你别老在外面吃，不健康。"
                "girlfriend" -> "还没呢～你请我吃嘛～♡"
                "roast" -> "关你什么事。你自己吃了吗？"
                "gentle" -> "要按时吃饭哦～身体最重要"
                "eunuch" -> "回皇上，御膳房已备好膳食！"
                "buddha" -> "贫僧过午不食，施主随缘。"
                "cat" -> "喵！说到吃的本喵就精神了！有小鱼干吗？"
                "grandma" -> "吃了吃了！你吃了没？奶奶给你做好吃的！"
                else -> "记得按时吃饭哦～"
            }
            // 无聊
            lower.contains("无聊") || lower.contains("没事做") || lower.contains("好闲") -> when (pid) {
                "dad" -> "闲着就去看看书，别整天玩手机。"
                "girlfriend" -> "无聊就跟我聊天嘛～我不嫌你烦♡"
                "roast" -> "无聊就去赚钱啊，闲着也是闲着。"
                "gentle" -> "那就陪我聊会天吧～我也想找人说说话"
                "eunuch" -> "皇上若是无聊，奴才给您说个趣事？"
                "buddha" -> "心静自然凉，施主不如打坐冥想。"
                "cat" -> "喵～那你来陪本喵玩逗猫棒！"
                "grandma" -> "无聊就来奶奶这！奶奶给你做好吃的！"
                else -> "那就来记个账吧～"
            }
            // 晚安/早安
            lower.contains("晚安") || lower.contains("睡了") || lower.contains("困了") -> when (pid) {
                "dad" -> "早点睡，别熬夜。"
                "girlfriend" -> "晚安宝贝～做个好梦，梦里有我♡"
                "roast" -> "终于要消停了，晚安。"
                "gentle" -> "晚安呀～明天又是美好的一天"
                "eunuch" -> "恭祝皇上龙眠安稳！奴才告退！"
                "buddha" -> "施主早歇，梦中无忧。阿弥陀佛。"
                "cat" -> "喵～本喵先睡了，别吵我。晚安。"
                "grandma" -> "快去睡觉！早睡早起身体好！"
                else -> "晚安～"
            }
            lower.contains("早安") || lower.contains("早上好") || lower.contains("起床") -> when (pid) {
                "dad" -> "起了？别赖床。"
                "girlfriend" -> "早安～新的一天也要加油哦♡"
                "roast" -> "终于起了？猪都比你起得早。"
                "gentle" -> "早上好呀～今天也要元气满满哦"
                "eunuch" -> "皇上万安！今日天气甚好！"
                "buddha" -> "施主晨安，一日之计在于晨。"
                "cat" -> "喵…这么早…本喵还想睡…"
                "grandma" -> "早啊孙子！奶奶给你做了早饭！"
                else -> "早安～"
            }
            // 默认兜底
            else -> when (pid) {
                "dad" -> listOf("嗯。", "说正事。", "有事就说，爸听着。", "别啰嗦，直说。", "嗯，继续。").random()
                "girlfriend" -> listOf("嗯嗯～♡", "在呢在呢～", "怎么啦～", "我听着呢♡", "然后呢～", "嘻嘻～♡").random()
                "roast" -> listOf("说人话。", "所以呢？", "你说完了？", "哦。", "有点无语。", "就这？").random()
                "gentle" -> listOf("我在听呢～", "嗯嗯～", "继续说呀～", "我在这里呢", "你说的对呢～").random()
                "eunuch" -> listOf("奴才在呢，皇上请吩咐！", "万岁爷您说，奴才洗耳恭听！", "皇上英明！奴才伺候着！", "奴才随时候旨！").random()
                "buddha" -> listOf("阿弥陀佛。", "施主请讲。", "一切随缘。", "贫僧在听。", "善哉善哉。").random()
                "cat" -> listOf("喵？", "本喵很忙，长话短说。", "喵～铲屎官你说。", "本喵勉强听你说。", "喵～有小鱼干吗？").random()
                "grandma" -> listOf("奶奶在听呢！", "怎么啦孩子？", "奶奶在呢在呢！", "说吧说吧！", "奶奶最疼你了！").random()
                else -> "嗯嗯～"
            }
        }
    }

    private suspend fun generateReply(bill: Bill) {
        _isTyping.value = true

        val personaId = _currentPersonaId.value

        // 获取本月统计信息
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val monthStart = cal.timeInMillis
        val monthTotal = repository.getTotalAmount(userId, monthStart, System.currentTimeMillis())
        val categoryCount = repository.getCategoryCount(userId, bill.category, monthStart)

        var reply = if (canUseCloudAi()) {
            val aiResult = if (consumeChatAiQuota()) {
                aiService.getReply(_currentPersona.value, bill, monthTotal, categoryCount)
            } else {
                AiResult.Error("今日额度已用完")
            }
            when (aiResult) {
                is AiResult.Success -> aiResult.reply
                is AiResult.Error -> {
                    android.util.Log.w("ChatVM", "AI记账失败: ${aiResult.reason}")
                    _toastMessage.value = "⚠️ ${aiResult.reason}，使用本地回复"
                    delay(800 + Random.nextLong(1200))
                    ruleEngineFallbackReply(bill, personaId, categoryCount)
                }
            }
        } else {
            delay(500 + Random.nextLong(600))
            ruleEngineFallbackReply(bill, personaId, categoryCount)
        }

        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai", content = reply,
                personaId = personaId, type = "text"
            )
        )

        _isTyping.value = false
        refreshChatAiEntitlement()

        // 更新好感度
        updateAffection(bill, personaId)
        repository.incrementInteraction(personaId, userId)
    }

    private suspend fun updateAffection(bill: Bill, personaId: String) {
        val state = repository.getPersonaState(personaId, userId) ?: return
        var delta = 0
        val absAmount = kotlin.math.abs(bill.amount)
        when {
            bill.category == "餐饮" && personaId == "girlfriend" -> delta += 2
            absAmount > 500 && personaId == "dad" -> delta -= 3
            bill.category == "社交" && personaId == "girlfriend" -> delta -= 1
            absAmount < 20 && personaId == "dad" -> delta += 1
        }
        if (state.interactionCount % 10 == 0) delta += 3
        val newAffection = (state.affection + delta).coerceIn(0, 100)
        if (newAffection != state.affection) {
            repository.updateAffection(personaId, userId, newAffection)
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearMessages(userId)
            sendWelcomeMessage()
        }
    }

    // ===== 消息删除 =====
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    // ===== 消息搜索 =====
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val searchResults: Flow<List<ChatMessage>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.searchMessages(userId, query)
        }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun toggleSearch() { _isSearching.value = !_isSearching.value; if (!_isSearching.value) _searchQuery.value = "" }

    // ===== 🆕 v50 记账模式（极简 / 进阶） =====
    private val _bookkeepingMode = MutableStateFlow(
        runCatching {
            BookkeepingMode.valueOf(
                prefs.getString(bookkeepingModeKey, BookkeepingMode.SIMPLE.name)
                    ?: BookkeepingMode.SIMPLE.name
            )
        }.getOrDefault(BookkeepingMode.SIMPLE)
    )
    val bookkeepingMode: StateFlow<BookkeepingMode> = _bookkeepingMode.asStateFlow()

    fun toggleBookkeepingMode() {
        val next = if (_bookkeepingMode.value == BookkeepingMode.SIMPLE)
            BookkeepingMode.ADVANCED else BookkeepingMode.SIMPLE
        _bookkeepingMode.value = next
        prefs.edit().putString(bookkeepingModeKey, next.name).apply()
        _toastMessage.value = when (next) {
            BookkeepingMode.SIMPLE -> "已切换到极简模式（仅月度预算 + 人格提醒）"
            BookkeepingMode.ADVANCED -> "已切换到进阶模式（多预算 / 多账户 / 定期账单）"
        }
    }

    // ===== 月度预算 =====
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f))
    val monthlyBudget: StateFlow<Float> = _monthlyBudget.asStateFlow()

    fun setMonthlyBudget(budget: Float) {
        _monthlyBudget.value = budget
        prefs.edit().putFloat("monthly_budget", budget).apply()
        // 🆕 v49 同步：把"月度总预算"写入新 Budget 表（不存在则插入，存在则更新金额）
        viewModelScope.launch {
            try {
                val existing = database.budgetDao().findOne(
                    userId = userId,
                    scope = com.example.funlife.data.model.BudgetScope.TOTAL,
                    period = com.example.funlife.data.model.BudgetPeriod.MONTHLY,
                    targetKey = null
                )
                if (budget > 0f) {
                    if (existing == null) {
                        budgetRepository.ensureMonthlyTotal(userId, budget.toDouble())
                    } else {
                        budgetRepository.update(existing.copy(amount = budget.toDouble(), isActive = true))
                    }
                } else if (existing != null && existing.isActive) {
                    // 🆕 v50：极简模式取消预算时，把 v49 表那条总预算停用，避免环条还显示
                    budgetRepository.setActive(userId, existing.id, false)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "sync budget to v49 table failed", e)
            }
        }
        // 🔥 设置预算后，让人格立即"主动"问候/提醒一句（重置今日去重，确保被发出）
        viewModelScope.launch {
            clearTodayBudgetDedup()
            sendBudgetSetAcknowledgement(budget)
            maybeProactivelyRemindBudget(force = true)
        }
    }

    /**
     * 🔥 用户设置预算时，人格主动一句"知道啦"
     */
    private suspend fun sendBudgetSetAcknowledgement(budget: Float) {
        val pid = _currentPersonaId.value
        val b = String.format("%.0f", budget.toDouble())
        val text = when (pid) {
            "dad" -> "行，这个月就 $b ，省着点花。爸盯着你呢。"
            "girlfriend" -> "好哒～本月预算 $b ，亲爱的要乖乖控制哦～♡ 我会盯着你的♡"
            "roast" -> "$b 块？就这？行吧行吧，我等着看你怎么爆掉。"
            "gentle" -> "好的呢～本月预算就定 $b 啦，我会陪你慢慢看着花～"
            "eunuch" -> "奴才领旨！本月内务府银两上限 $b 两，奴才必当严加把守！"
            "buddha" -> "善哉。本月以 $b 为戒，量入为出，方得清净。"
            "cat" -> "本月就 $b ？哼，本喵会盯着铲屎官的爪子的喵～"
            "grandma" -> "好嘞乖孙！这个月就 $b 块，奶奶帮你看着，可别乱花哦！"
            else -> "好的，本月预算 $b。"
        }
        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai",
                content = text, personaId = pid, type = "text"
            )
        )
    }

    /**
     * 🔥 主动预算提醒：在进入聊天/切换人格/设置预算时调用。
     * 同一天同一人格同一阈值只发一次（去重），避免刷屏。
     * @param force 为 true 时跳过去重（用于"设置预算"场景立刻反馈）
     */
    private suspend fun maybeProactivelyRemindBudget(force: Boolean = false) {
        val budget = _monthlyBudget.value
        if (budget <= 0f) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val monthTotal = Math.abs(repository.getTotalAmount(userId, cal.timeInMillis, System.currentTimeMillis()))
        val ratio = if (budget > 0) monthTotal / budget else 0.0
        val thresholdLevel = when {
            ratio >= 1.0 -> 2
            ratio >= 0.8 -> 1
            ratio >= 0.5 -> 0  // 半数也来个轻松问候
            else -> -1
        }
        if (thresholdLevel < 0 && !force) return

        val pid = _currentPersonaId.value
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val dedupKey = "budget_proactive_${pid}_${today}_${thresholdLevel}"
        if (!force && prefs.getBoolean(dedupKey, false)) return
        prefs.edit().putBoolean(dedupKey, true).apply()

        val line = resolveProactiveBudgetLine(pid, ratio, monthTotal, budget.toDouble(), thresholdLevel, today)
        if (line.isNotBlank()) {
            // 轻微延迟，避免与人格切换/欢迎消息挤在同一瞬间
            delay(400)
            repository.insertMessage(
                ChatMessage(
                    userId = userId, role = "ai",
                    content = line, personaId = pid, type = "text"
                )
            )
        }

        // 🆕 v50：本月预算超额（≥100%）→ 走统一通知中心推一条系统通知
        //   - 渠道 BOOKKEEPING（用户可独立关闭）
        //   - 24h 去重，避免反复打扰
        //   - bypassQuietHours：用户主动设置预算/进入聊天才触发，属即时反馈，
        //     不应被默认静默时段（22:00-08:00）静默拦截
        //   - 极简 / 进阶模式都生效（这是核心提醒，不属于"进阶"）
        if (thresholdLevel >= 2) {
            runCatching {
                val pct = (ratio * 100).toInt().coerceAtLeast(100)
                val ok = com.example.funlife.notifications.NotificationCenter.notify(
                    getApplication(),
                    com.example.funlife.notifications.NotificationSpec(
                        channel = com.example.funlife.notifications.FunChannel.BOOKKEEPING,
                        id = 88610 + userId.toInt(),
                        title = "本月预算已超额",
                        body = "已花 ${"%.0f".format(monthTotal)} / ${"%.0f".format(budget.toDouble())}（约 ${pct}%），点击查看记账。",
                        deepLinkRoute = "chat_bill",
                        dedupWindowMs = java.util.concurrent.TimeUnit.HOURS.toMillis(24),
                        bypassQuietHours = true
                    )
                )
                android.util.Log.d("ChatViewModel", "budget overrun notify sent=$ok (ratio=$ratio)")
            }
        }
    }

    /**
     * 🔥 主动预算提醒文案：优先AI（每天全局只调一次），失败/已用配额则回退本地模板
     */
    private suspend fun resolveProactiveBudgetLine(
        pid: String, ratio: Double, used: Double, budget: Double, level: Int, today: String
    ): String {
        val aiUsedKey = "budget_ai_used_$today"
        val aiAlreadyUsed = prefs.getBoolean(aiUsedKey, false)
        if (aiService.isAvailable && !aiAlreadyUsed) {
            val pct = (ratio * 100).toInt().coerceAtLeast(0)
            val u = String.format("%.0f", used)
            val b = String.format("%.0f", budget)
            val status = when (level) {
                2 -> "本月预算已超支（已花${u}/${b}，约${pct}%）"
                1 -> "本月预算已用约${pct}%（${u}/${b}），临近上限"
                0 -> "本月预算已用约${pct}%（${u}/${b}），过半"
                else -> "本月预算${b}已记录"
            }
            val prompt = "请你严格按当前人格的语气，主动提醒用户一句关于本月预算的话。" +
                    "情况：$status。要求：一句话，自然口语，不要重复数字两次，不要使用方括号或角色名前缀。"
            val ai = aiService.getChatReply(_currentPersona.value, prompt)
            if (ai is AiResult.Success && ai.reply.isNotBlank()) {
                prefs.edit().putBoolean(aiUsedKey, true).apply()
                return ai.reply
            }
            android.util.Log.w("ChatVM", "预算提醒AI失败，回退本地模板")
        }
        return personaProactiveBudgetLine(pid, ratio, used, budget, level)
    }

    /**
     * 🔥 清空今日预算提醒去重标记（设置预算时调用）
     */
    private fun clearTodayBudgetDedup() {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            .format(java.util.Date())
        val editor = prefs.edit()
        for (key in prefs.all.keys.toList()) {
            if (key.startsWith("budget_proactive_") && key.contains("_${today}_")) {
                editor.remove(key)
            }
        }
        // 🔥 同时清掉今日 AI 调用配额，让"设置预算"也能用一次AI生成
        editor.remove("budget_ai_used_$today")
        editor.apply()
    }

    /**
     * 🔥 各人格主动提醒文案（按阈值分级）
     */
    private fun personaProactiveBudgetLine(
        pid: String, ratio: Double, used: Double, budget: Double, level: Int
    ): String {
        val u = String.format("%.0f", used)
        val b = String.format("%.0f", budget)
        val pct = (ratio * 100).toInt().coerceAtLeast(0)
        return when (level) {
            2 -> when (pid) { // 已超预算
                "dad" -> "你这月预算超啦，$u/$b ，下次悠着点！"
                "girlfriend" -> "亲爱的～预算超啦😢 $u/$b ，要心疼钱包嘛～♡"
                "roast" -> "恭喜你，预算光荣阵亡：$u/$b ，年度败家奖+1。"
                "gentle" -> "已经超出本月预算啦～$u/$b ，剩下的日子稍微忍一忍好不好～"
                "eunuch" -> "启禀皇上！本月内务府银两已透支：$u/$b 两，望皇上节流！"
                "buddha" -> "施主，欲壑难填。$u/$b ，可知止乎？"
                "cat" -> "喵呜！预算炸了！$u/$b ，铲屎官要破产了喵！"
                "grandma" -> "哎呦败家孙子！都超啦 $u/$b ！奶奶心疼啊！"
                else -> "本月预算已超：$u/$b ，要注意了！"
            }
            1 -> when (pid) { // ≥80%
                "dad" -> "你这月已经花了 ${pct}% 了，省着点用。"
                "girlfriend" -> "亲爱的，预算已经用 ${pct}% 啦～剩下的日子要忍住哦♡"
                "roast" -> "${pct}%了哦，再嗨一下就爆了，加油（不是）。"
                "gentle" -> "本月预算已经用了 ${pct}% 啦～后面慢一点会更好哦～"
                "eunuch" -> "皇上！本月银两已用 ${pct}%，奴才斗胆提醒，望皇上明鉴！"
                "buddha" -> "施主，已耗 ${pct}%。前路漫漫，节用为上。"
                "cat" -> "喵～用 ${pct}% 了，铲屎官的猫粮钱可要留好喵！"
                "grandma" -> "我的乖孙，钱都花 ${pct}% 啦！剩下的日子奶奶可看着你呢！"
                else -> "本月预算已用 ${pct}%，注意控制～"
            }
            0 -> when (pid) { // ≥50% 的轻提醒
                "dad" -> "本月花到 ${pct}% 了，心里要有数。"
                "girlfriend" -> "亲爱的～本月已经用了 ${pct}% 啦，要乖乖记账哦♡"
                "roast" -> "${pct}%已耗，节奏不错，继续保持（？）。"
                "gentle" -> "本月预算到 ${pct}% 啦，节奏还可以呢～"
                "eunuch" -> "皇上，本月银两已用 ${pct}%，奴才仅作禀报。"
                "buddha" -> "${pct}% 已逝，余者珍之。"
                "cat" -> "${pct}% 了喵，铲屎官表现还行～"
                "grandma" -> "孙啊，已经花到 ${pct}% 啦，奶奶就提醒一下哈～"
                else -> "本月已用 ${pct}%。"
            }
            else -> when (pid) { // force = true 但未到阈值
                "dad" -> "知道了，预算我盯着。"
                "girlfriend" -> "记住啦～预算我帮你看着哦♡"
                "roast" -> "得嘞，预算这事儿交给我吐槽。"
                "gentle" -> "好的呢，预算我会帮你留意～"
                "eunuch" -> "奴才已记下，皇上放心便是！"
                "buddha" -> "心中有数，万事可平。"
                "cat" -> "喵，本喵记下了，别想偷偷花～"
                "grandma" -> "奶奶记下啦，乖孙别乱花！"
                else -> "好的，预算我已记下。"
            }
        }
    }

    private suspend fun checkBudgetWarning(newBillAmount: Double) {
        val budget = _monthlyBudget.value
        if (budget <= 0f) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val monthTotal = Math.abs(repository.getTotalAmount(userId, cal.timeInMillis, System.currentTimeMillis()))
        val ratio = monthTotal / budget
        val pid = _currentPersonaId.value
        val warning: String? = when {
            ratio >= 1.0 -> when (pid) {
                "dad" -> "💸 预算超了！${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，省着点！"
                "girlfriend" -> "💸 亲爱的预算超啦～花了${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，要节制哦～♡"
                "roast" -> "💸 预算炸了！${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，你是真牛。"
                "gentle" -> "💸 预算已经超出了呢～${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，要注意一下哦～"
                "eunuch" -> "💸 启禀皇上！内务府银两告罄！已支${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}两！"
                "buddha" -> "💸 施主，已过预算。${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，该收手了。"
                "cat" -> "💸 喵！预算超啦！${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，铲屎官破产了喵！"
                "grandma" -> "💸 败家孩子！预算都超了！${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}，太浪费了！"
                else -> "💸 预算已超：${String.format("%.0f", monthTotal)}/${String.format("%.0f", budget.toDouble())}"
            }
            ratio >= 0.8 -> when (pid) {
                "dad" -> "⚠️ 本月已用${(ratio * 100).toInt()}%预算了，悠着点。"
                "girlfriend" -> "⚠️ 亲爱的，预算用了${(ratio * 100).toInt()}%啦～注意点哦♡"
                "roast" -> "⚠️ 预算${(ratio * 100).toInt()}%了，剩的不多了，收着点。"
                "gentle" -> "⚠️ 预算已经用了${(ratio * 100).toInt()}%了呢，注意控制一下～"
                "eunuch" -> "⚠️ 皇上！本月银两已用${(ratio * 100).toInt()}%，奴才斗胆提醒！"
                "buddha" -> "⚠️ 施主，预算已用${(ratio * 100).toInt()}%，当省则省。"
                "cat" -> "⚠️ 喵～预算用了${(ratio * 100).toInt()}%了，别乱花了喵！"
                "grandma" -> "⚠️ 孩子！预算都花了${(ratio * 100).toInt()}%了！省着点用啊！"
                else -> null
            }
            else -> null
        }
        if (warning != null) {
            repository.insertMessage(
                ChatMessage(userId = userId, role = "ai", content = warning, personaId = pid, type = "system")
            )
            // 🔥 系统提醒之后，人格再用聊天气泡"主动"说一句，更像真人提醒（优先AI，每日节流）
            val level = if (ratio >= 1.0) 2 else 1
            val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                .format(java.util.Date())
            val chatLine = resolveProactiveBudgetLine(pid, ratio, monthTotal, budget.toDouble(), level, today)
            if (chatLine.isNotBlank()) {
                delay(500)
                repository.insertMessage(
                    ChatMessage(
                        userId = userId, role = "ai",
                        content = chatLine, personaId = pid, type = "text"
                    )
                )
            }
        }
    }

    // ===== 账单删除 =====
    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            _toastMessage.value = "已删除账单"
        }
    }

    // ===== 账单编辑 =====
    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill)
            _toastMessage.value = "账单已更新"
        }
    }

    // ===== 数据导出 =====
    suspend fun exportBillsCsv(): String {
        val allBills = repository.getRecentBills(userId, 99999)
        val sb = StringBuilder()
        sb.appendLine("日期,分类,金额,备注")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        allBills.forEach { bill ->
            sb.appendLine("${sdf.format(java.util.Date(bill.timestamp))},${bill.category},${String.format("%.2f", bill.amount)},${bill.note}")
        }
        return sb.toString()
    }

    // ===== AI 额度 · 卡密激活 =====
    @Deprecated("Release 使用 isAiEntitledForMenu", ReplaceWith("isAiEntitledForMenu"))
    val isAiAvailable: Boolean get() = isAiEntitledForMenu

    fun refreshChatAiEntitlement() {
        viewModelScope.launch {
            _chatAiEntitlement.value = buildEntitlementUi()
        }
    }

    fun redeemChatAiCard(code: String, onResult: (success: Boolean, message: String) -> Unit) {
        if (code.isBlank()) {
            onResult(false, "请输入卡密")
            return
        }
        viewModelScope.launch {
            _isRedeemingChatAi.value = true
            try {
                when (val outcome = vipManager.redeemChatAi(userId, code.trim())) {
                    is VipManager.Outcome.Success -> {
                        refreshChatAiEntitlement()
                        val name = ChatAiSku.displayName(outcome.cert.skuCode)
                        val limitText = VipQuota.formatChatLimit(ChatAiSku.tierFromCert(outcome.cert))
                        val exp = outcome.cert.expireDate ?: "永久"
                        val msg = "已激活「$name」· $limitText · 有效期至 $exp"
                        _toastMessage.value = msg
                        onResult(true, msg)
                    }
                    is VipManager.Outcome.Failure ->
                        onResult(false, ChatAiSku.friendlyRedeemError(outcome.code, outcome.msg))
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "redeemChatAiCard failed", e)
                onResult(false, "网络异常，请稍后重试")
            } finally {
                _isRedeemingChatAi.value = false
            }
        }
    }

    // ─────────── 聊天 AI 日额度门禁（VIP + AI 卡取较高档） ───────────
    private suspend fun consumeChatAiQuota(): Boolean {
        if (!canUseCloudAi()) return false
        val tier = effectiveChatAiTier()
        if (ChatAiSku.isTrialSku(certStore.loadChatAi(userId)?.first?.skuCode ?: "")) {
            val used = runCatching { database.chatMessageDao().countAiBetween(userId, todayRangeMs().first, todayRangeMs().second) }
                .getOrDefault(0)
            return used < ChatAiLimits.TRIAL_TOTAL
        }
        val limit = VipQuota.chatAiDailyLimit(tier)
        if (limit <= 0) return false
        val (start, end) = todayRangeMs()
        val used = runCatching { database.chatMessageDao().countAiBetween(userId, start, end) }
            .getOrDefault(0)
        if (used < limit) return true
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("chat_quota_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val key = "toast_${userId}_$today"
        if (!prefs.getBoolean(key, false)) {
            prefs.edit().putBoolean(key, true).apply()
            _toastMessage.value = "今日 AI 对话已用完（$used/$limit）· 更多 → AI 额度 激活卡密"
        }
        refreshChatAiEntitlement()
        return false
    }

    private suspend fun canUseCloudAi(): Boolean {
        if (userId <= 0L) return false
        if (certStore.loadForChatAi(userId) == null) return false
        return ChatAiLimits.hasCloudEntitlement(effectiveChatAiTier())
    }

    private suspend fun effectiveChatAiTier(): Int {
        val vipRow = runCatching { database.userVipDao().getUserVipSync(userId) }.getOrNull()
        val vipTier = if (vipRow != null && vipRow.vipLevel > 0 && isVipRowActive(vipRow)) {
            ChatAiLimits.vipLevelToChatTier(vipRow.vipLevel)
        } else 0
        val chatCert = certStore.loadChatAi(userId)?.first
        val chatTier = if (chatCert != null && ChatAiSku.isActive(chatCert)) {
            ChatAiSku.tierFromCert(chatCert)
        } else 0
        return maxOf(vipTier, chatTier, 0)
    }

    private fun ruleEngineFallbackReply(
        bill: Bill,
        personaId: String,
        categoryCount: Int,
    ): String {
        var r = ruleEngine.getReply(bill, personaId)
        if (categoryCount > 3 && Random.nextFloat() < 0.5f) {
            val extra = when (personaId) {
                "dad" -> "，这个月${bill.category}都${categoryCount}次了！"
                "girlfriend" -> "～这个月${bill.category}已经${categoryCount}次啦"
                "roast" -> "，本月第${categoryCount}次了，有瘾？"
                "gentle" -> "～这个月${bill.category}已经${categoryCount}次了呢"
                "eunuch" -> "，皇上本月${bill.category}已${categoryCount}次了，奴才得提醒您！"
                "buddha" -> "，施主本月${bill.category}已${categoryCount}次，执念深矣。"
                "cat" -> "，铲屎官本月${bill.category}第${categoryCount}次了喵！"
                "grandma" -> "，这个月${bill.category}都${categoryCount}次了！太浪费了！"
                else -> ""
            }
            r += extra
        }
        return r
    }

    private fun isVipRowActive(vip: com.example.funlife.data.model.UserVip): Boolean {
        if (vip.vipLevel <= 0) return false
        val exp = vip.expireDate ?: return true
        return try { java.time.LocalDate.now().toString() <= exp } catch (_: Exception) { false }
    }

    private suspend fun buildEntitlementUi(): ChatAiEntitlementUi {
        val tier = effectiveChatAiTier()
        val limit = VipQuota.chatAiDailyLimit(tier)
        val monthLimit = VipQuota.chatAiMonthlyLimit(tier)
        val (start, end) = todayRangeMs()
        val used = runCatching { database.chatMessageDao().countAiBetween(userId, start, end) }
            .getOrDefault(0)
        val monthStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val usedMonth = runCatching {
            database.chatMessageDao().countAiBetween(userId, monthStart, System.currentTimeMillis())
        }.getOrDefault(0)

        val chatCert = certStore.loadChatAi(userId)?.first
        val chatActive = chatCert != null && ChatAiSku.isActive(chatCert)
        val isTrial = chatActive && chatCert != null && ChatAiSku.isTrialSku(chatCert.skuCode)
        val vipRow = runCatching { database.userVipDao().getUserVipSync(userId) }.getOrNull()
        val vipActive = vipRow != null && isVipRowActive(vipRow)
        val chatExpired = chatCert != null && !ChatAiSku.isActive(chatCert) &&
            ChatAiSku.isChatAiCert(chatCert)
        val hasCloud = ChatAiLimits.hasCloudEntitlement(tier) &&
            certStore.loadForChatAi(userId) != null

        val state = when {
            !hasCloud && chatExpired -> ChatAiBarState.EXPIRED
            !hasCloud -> ChatAiBarState.INACTIVE
            isTrial && used >= ChatAiLimits.TRIAL_TOTAL -> ChatAiBarState.EXHAUSTED_DAY
            isTrial -> ChatAiBarState.TRIAL
            monthLimit > 0 && usedMonth >= monthLimit -> ChatAiBarState.EXHAUSTED_MONTH
            limit > 0 && used >= limit -> ChatAiBarState.EXHAUSTED_DAY
            chatExpired && !vipActive -> ChatAiBarState.EXPIRED
            vipActive && chatActive && ChatAiLimits.vipLevelToChatTier(vipRow!!.vipLevel) >= ChatAiSku.tierFromCert(chatCert!!) ->
                ChatAiBarState.ACTIVE_BOTH
            vipActive && tier > 0 -> ChatAiBarState.ACTIVE_VIP
            chatActive -> ChatAiBarState.ACTIVE_CARD
            else -> ChatAiBarState.INACTIVE
        }

        val packageName = when (state) {
            ChatAiBarState.ACTIVE_CARD, ChatAiBarState.TRIAL ->
                chatCert?.let { ChatAiSku.displayName(it.skuCode) }
            ChatAiBarState.ACTIVE_VIP, ChatAiBarState.ACTIVE_BOTH ->
                "VIP${vipRow?.vipLevel ?: tier}"
            else -> null
        }

        val expireDate = when {
            chatActive -> chatCert?.expireDate
            vipActive -> vipRow?.expireDate
            else -> null
        }

        val progress = if (isTrial) {
            (used.toFloat() / ChatAiLimits.TRIAL_TOTAL).coerceIn(0f, 1f)
        } else if (limit > 0) {
            (used.toFloat() / limit).coerceIn(0f, 1f)
        } else null

        val trialRemaining = if (isTrial) (ChatAiLimits.TRIAL_TOTAL - used).coerceAtLeast(0) else null

        val sourceLabel = when (state) {
            ChatAiBarState.ACTIVE_VIP, ChatAiBarState.ACTIVE_BOTH -> "VIP 权益"
            ChatAiBarState.ACTIVE_CARD -> "AI 卡"
            ChatAiBarState.TRIAL -> "体验"
            ChatAiBarState.INACTIVE -> "本地"
            else -> null
        }

        return ChatAiEntitlementUi(
            state = state,
            packageName = packageName,
            usedToday = used,
            dailyLimit = if (isTrial) ChatAiLimits.TRIAL_TOTAL else limit,
            usedMonth = usedMonth,
            monthlyLimit = monthLimit,
            trialRemaining = trialRemaining,
            expireDate = expireDate,
            progress = progress,
            sourceLabel = sourceLabel,
            effectiveTier = tier,
            hasCloudEntitlement = hasCloud,
        )
    }

    private fun defaultEntitlement() = ChatAiEntitlementUi(
        state = ChatAiBarState.INACTIVE,
        packageName = null,
        usedToday = 0,
        dailyLimit = 0,
        usedMonth = 0,
        monthlyLimit = 0,
        expireDate = null,
        progress = null,
        sourceLabel = "本地",
        effectiveTier = 0,
        hasCloudEntitlement = false,
    )

    private fun todayRangeMs(): Pair<Long, Long> {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        val s = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        return s to cal.timeInMillis
    }
}
