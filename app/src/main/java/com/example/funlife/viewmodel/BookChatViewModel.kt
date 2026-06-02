// BookChatViewModel.kt — v53 阅光书房 · AI 读书伴侣
//
// 设计：
//   - 普通 / VIP1 / VIP2：对话仅内存，不持久化（隐私 + 不解锁深聊）
//   - VIP3 / 永久会员：自动落库到 BookChatSession，用户可在 BookDetail 页查看历史档案
//   - 每轮调云端 chat_ai mode=book，client 端拼装 system prompt 包含书目摘要 + 用户摘抄
//   - 配额由云端权威判定；返回 used/limit 显示在 UI
//   - 加载已有 session：传入 sessionId > 0 即可恢复对话
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.FunLifeApplication
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.BookChatSession
import com.example.funlife.data.model.Quote
import com.example.funlife.vip.ChatAiCloudRepository
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BookChatViewModel(
    application: Application,
    val userId: Long,
    val bookId: Long,
    /** > 0 表示加载已有档案；0 / 默认 = 开新对话 */
    val initialSessionId: Long = 0L,
) : AndroidViewModel(application) {

    private val app = application as FunLifeApplication
    private val db = app.database
    private val gson = Gson()

    /** 「>3 轮」深聊门控 + 长对话存档：VIP3 / 永久会员才能解锁。
     *  init 一次性快照；send/persist 时通过 [refreshVipState] 实时刷新，避免用户中途升 VIP 不生效。 */
    @Volatile private var deepChatUnlocked: Boolean = false
    /** 当前持久化的 session id（仅 VIP3 才会有；普通用户始终 = 0）。 */
    @Volatile private var currentSessionId: Long = 0L
    /** 互斥锁：防并发 persistSession 重复 insert 多条 session（用户连点两次 send 边界）。 */
    private val persistMutex = Mutex()

    sealed class Msg {
        data class User(val text: String) : Msg()
        data class Ai(val text: String) : Msg()
        data class System(val text: String) : Msg()
    }

    /** 持久化用：固定字段 schema，messagesJson 反序列化目标。 */
    private data class StoredMsg(
        @SerializedName("role") val role: String,
        @SerializedName("text") val text: String,
        @SerializedName("ts") val ts: Long,
    )

    private val _msgs = MutableStateFlow<List<Msg>>(emptyList())
    val msgs: StateFlow<List<Msg>> = _msgs.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _quota = MutableStateFlow<Pair<Int, Int>>(0 to -1) // used to limit
    val quota: StateFlow<Pair<Int, Int>> = _quota.asStateFlow()

    private val _bookTitle = MutableStateFlow("")
    val bookTitle: StateFlow<String> = _bookTitle.asStateFlow()

    private var book: Book? = null
    private var quotes: List<Quote> = emptyList()

    init {
        viewModelScope.launch {
            val vipLevel = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
            deepChatUnlocked = com.example.funlife.vip.VipQuota.aiBookDeepChatUnlocked(vipLevel)
            book = db.bookDao().getById(userId, bookId)
            _bookTitle.value = book?.title ?: ""
            quotes = db.quoteDao().getRecentByBook(userId, bookId, limit = 8)

            // 优先尝试加载已有档案（仅 VIP3 用户传 initialSessionId 才有意义）
            val loaded = if (deepChatUnlocked && initialSessionId > 0L) {
                db.bookChatSessionDao().getById(userId, initialSessionId)
            } else null

            if (loaded != null) {
                currentSessionId = loaded.id
                val historyMsgs = decodeMessages(loaded.messagesJson)
                _msgs.value = listOf(
                    Msg.System("📒 这是你 ${formatDate(loaded.lastMessageAt)} 的对话档案，可继续聊下去。")
                ) + historyMsgs
            } else {
                _msgs.value = listOf(
                    Msg.System(
                        "在这里聊聊《${book?.title ?: "这本书"}》。" +
                        if (quotes.isNotEmpty()) "\n（已带入你写下的 ${quotes.size} 条摘抄作为上下文）"
                        else "\n（先在书的详情页抄几句你舍不得忘的话，AI 会读懂你更深一些。）"
                    )
                )
            }
        }
    }

    /** 把 storage JSON 转回 Msg 列表（system 消息不存储，仅存 user/ai）。 */
    private fun decodeMessages(json: String): List<Msg> {
        return runCatching {
            val arr = gson.fromJson(json, Array<StoredMsg>::class.java) ?: return@runCatching emptyList<Msg>()
            arr.mapNotNull {
                when (it.role) {
                    "user" -> Msg.User(it.text)
                    "ai" -> Msg.Ai(it.text)
                    else -> null
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Msg 列表 → JSON（仅持久化 user/ai；system 不入库）。 */
    private fun encodeMessages(): String {
        val now = System.currentTimeMillis()
        val list = _msgs.value.mapNotNull { m ->
            when (m) {
                is Msg.User -> StoredMsg("user", m.text, now)
                is Msg.Ai -> StoredMsg("ai", m.text, now)
                is Msg.System -> null
            }
        }
        return gson.toJson(list)
    }

    private fun formatDate(ms: Long): String =
        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(ms))

    /** 实时刷新 VIP 状态（用户中途升档时立即生效）。 */
    private suspend fun refreshVipState() {
        val vipLevel = db.userVipDao().getUserVipSync(userId)?.vipLevel ?: 0
        deepChatUnlocked = com.example.funlife.vip.VipQuota.aiBookDeepChatUnlocked(vipLevel)
    }

    /**
     * 持久化当前会话（仅 VIP3 调用）。
     * 用 Mutex 保证并发 send 不会同时 insert 出两条 session。
     */
    private suspend fun persistSession() {
        refreshVipState()
        if (!deepChatUnlocked) return
        persistMutex.withLock {
            val turns = _msgs.value.count { it is Msg.User }
            if (turns == 0) return@withLock  // 还没人开口
            val now = System.currentTimeMillis()
            val title = _msgs.value.firstOrNull { it is Msg.User }
                ?.let { (it as Msg.User).text.take(20) } ?: "未命名对话"
            val payload = encodeMessages()
            if (currentSessionId > 0L) {
                val cur = db.bookChatSessionDao().getById(userId, currentSessionId) ?: return@withLock
                db.bookChatSessionDao().update(
                    cur.copy(
                        title = if (cur.title.isBlank()) title else cur.title,
                        messagesJson = payload,
                        turnCount = turns,
                        lastMessageAt = now,
                    )
                )
            } else {
                currentSessionId = db.bookChatSessionDao().insert(
                    BookChatSession(
                        userId = userId, bookId = bookId,
                        title = title, messagesJson = payload, turnCount = turns,
                        createdAt = now, lastMessageAt = now,
                    )
                )
            }
        }
    }

    fun send(userText: String) {
        val txt = userText.trim()
        if (txt.isEmpty() || _sending.value) return

        _sending.value = true
        _msgs.value = _msgs.value + Msg.User(txt)
        viewModelScope.launch {
            // 实时刷新 VIP 状态（中途升档立即生效）
            refreshVipState()
            // 深聊门控：非 VIP3 / 永久会员，单次会话超过 3 轮即拒绝再回复
            val priorUserTurnsBeforeThis = _msgs.value.count { it is Msg.User } - 1
            if (priorUserTurnsBeforeThis >= 3 && !deepChatUnlocked) {
                _msgs.value = _msgs.value + Msg.System(
                    "🔒 单次对话已经聊到第 ${priorUserTurnsBeforeThis + 1} 轮——继续多轮深聊是 VIP3 / 永久会员的专属。\n" +
                    "你可以新建对话从头开始，或在「我的 → VIP」里升级解锁。"
                )
                _sending.value = false
                return@launch
            }
            val cloud = ChatAiCloudRepository(getApplication())
            val systemPrompt = buildSystem()
            val r = cloud.reply(
                userId = userId,
                body = ChatAiCloudRepository.Body(
                    mode = "book",
                    personaSystem = systemPrompt,
                    userText = buildUserPayload(txt)
                )
            )
            when (r) {
                is ChatAiCloudRepository.CallResult.Success -> {
                    _msgs.value = _msgs.value + Msg.Ai(r.reply)
                    _quota.value = r.used to r.limit
                    persistSession()  // 🆕 v54 VIP3 自动落档
                }
                is ChatAiCloudRepository.CallResult.QuotaExceeded -> {
                    _msgs.value = _msgs.value + Msg.System(
                        "今日 AI 读书伴侣已用完（${r.used}/${r.limit}）。明天再来，或升级 VIP 解锁更多次数。"
                    )
                    _quota.value = r.used to r.limit
                }
                is ChatAiCloudRepository.CallResult.Rejected ->
                    _msgs.value = _msgs.value + Msg.System("请求被拒绝：${r.msg}")
                is ChatAiCloudRepository.CallResult.Recoverable ->
                    _msgs.value = _msgs.value + Msg.System("网络异常：${r.msg}，稍后重试")
            }
            _sending.value = false
        }
    }

    private fun buildSystem(): String = """
        你是用户的"读书伴侣"，温暖、克制、有洞察力，像一个比用户多读了一些书的朋友。
        风格要求：
        - 不要居高临下解释作品，更不要剧透。
        - 接住用户的情绪和疑问，必要时反问，让对话更深一层。
        - 引用用户的摘抄时要自然，不要逐字复述。
        - 中文回答，每次 2-4 段，不要过长。
    """.trimIndent()

    private fun buildUserPayload(currentInput: String): String = buildString {
        append("当前正在聊的书：《${book?.title ?: "未知"}》")
        book?.author?.takeIf { it.isNotBlank() }?.let { append(" / 作者 $it") }
        if (book?.openingLetter?.isNotBlank() == true) {
            append("\n用户在翻开这本书时写过：${book?.openingLetter}")
        }
        if (quotes.isNotEmpty()) {
            append("\n用户写下的摘抄（按时间倒序）：")
            quotes.take(8).forEachIndexed { i, q ->
                append("\n  ${i + 1}) ${q.text.take(120)}")
            }
        }
        // 最近 6 轮对话作为上下文（不含 system 消息）
        val history = _msgs.value
            .filter { it !is Msg.System }
            .takeLast(6)
        if (history.isNotEmpty()) {
            append("\n\n最近的对话：")
            history.forEach { m ->
                when (m) {
                    is Msg.User -> append("\n用户：${m.text}")
                    is Msg.Ai -> append("\nAI：${m.text}")
                    is Msg.System -> {}
                }
            }
        }
        append("\n\n用户的最新提问：$currentInput")
    }
}
