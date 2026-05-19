package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.ChatMessage
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.data.model.ChatPersonaState
import com.example.funlife.repository.ChatRepository
import com.example.funlife.utils.AiResult
import com.example.funlife.utils.AiService
import com.example.funlife.utils.RuleEngine
import com.example.funlife.utils.parseBillInput
import android.content.Context
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

@OptIn(FlowPreview::class)
class ChatViewModel(application: Application, val userId: Long) : AndroidViewModel(application) {

    private val database = (application as FunLifeApplication).database
    private val repository = ChatRepository(
        database.billDao(),
        database.chatMessageDao(),
        database.chatPersonaDao()
    )
    private val ruleEngine = RuleEngine()
    // 🔒 安全修复：把 userId 传给 AiService，让 API Key 按账号加密隔离存储
    private val aiService = AiService(application, userId)

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
        initializePersonas()
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
            val parsed = parseBillInput(input)
            if (parsed != null) {
                // 记账消息
                addBill(parsed.amount, parsed.category, parsed.note, input)
            } else {
                // 普通聊天消息
                sendChatMessage(input)
            }
        }
    }

    private suspend fun addBill(amount: Double, category: String, note: String, rawInput: String) {
        // 1. 保存账单
        val bill = Bill(userId = userId, amount = amount, category = category, note = note)
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

        // 策略：先尝试AI，失败则用本地关键词回复
        val aiResult = aiService.getChatReply(_currentPersona.value, text)
        val reply = when (aiResult) {
            is AiResult.Success -> aiResult.reply
            is AiResult.Error -> {
                android.util.Log.w("ChatVM", "AI聊天失败: ${aiResult.reason}")
                _toastMessage.value = "⚠️ AI: ${aiResult.reason}，使用本地回复"
                delay(600 + Random.nextLong(800))
                getLocalChatReply(text)
            }
        }

        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai", content = reply,
                personaId = _currentPersonaId.value, type = "text"
            )
        )
        _isTyping.value = false

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

        // 策略：先尝试AI，失败则用规则引擎
        val aiResult = aiService.getReply(
            _currentPersona.value, bill, monthTotal, categoryCount
        )
        var reply = when (aiResult) {
            is AiResult.Success -> aiResult.reply
            is AiResult.Error -> {
                android.util.Log.w("ChatVM", "AI记账失败: ${aiResult.reason}")
                _toastMessage.value = "⚠️ AI: ${aiResult.reason}，使用本地回复"
                delay(800 + Random.nextLong(1200))
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
                r
            }
        }

        repository.insertMessage(
            ChatMessage(
                userId = userId, role = "ai", content = reply,
                personaId = personaId, type = "text"
            )
        )

        _isTyping.value = false

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

    // ===== 月度预算 =====
    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f))
    val monthlyBudget: StateFlow<Float> = _monthlyBudget.asStateFlow()

    fun setMonthlyBudget(budget: Float) {
        _monthlyBudget.value = budget
        prefs.edit().putFloat("monthly_budget", budget).apply()
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

    // ===== AI设置 =====
    val isAiAvailable: Boolean get() = aiService.isAvailable

    fun getAiApiKey(): String = aiService.getApiKey()

    fun setAiApiKey(key: String) {
        aiService.setApiKey(key)
    }
}
