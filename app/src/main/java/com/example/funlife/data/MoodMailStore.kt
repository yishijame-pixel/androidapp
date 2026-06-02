package com.example.funlife.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 心情邮件 —— 系统根据用户情绪状态推送的鼓励 / 提醒卡片。
 *
 * 状态机：
 *   未签收（accepted=false） -> 列表显示密封信封，需点击「签收」
 *   已签收未读（accepted=true, read=false） -> 列表显示打开的信封 + 红点
 *   已读（accepted=true, read=true） -> 列表显示已读
 */
data class MoodMail(
    val id: String,
    val type: String,           // 模板 id，用于去重防止重复推送
    val emoji: String,
    val title: String,
    val body: String,
    val accentHex: Long,        // 颜色 0xFFRRGGBB
    val createdAt: Long,        // 推送时间（毫秒）
    val accepted: Boolean = false,
    val read: Boolean = false
)

object MoodMailStore {
    private const val PREF = "mood_mail_store"
    private const val KEY_LIST = "mails"
    private const val KEY_LAST_PUSH = "last_push_"  // last_push_<type> = millis

    // ── 响应式列表 + 未读计数：UI 用 collectAsState 自动随消息变化更新 ──
    private val _mailsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<MoodMail>>(emptyList())
    val mailsFlow: kotlinx.coroutines.flow.StateFlow<List<MoodMail>> = _mailsFlow
    private val _unreadFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
    val unreadFlow: kotlinx.coroutines.flow.StateFlow<Int> = _unreadFlow

    /** UI 重新可见时调用，刷新最新状态 */
    fun refresh(ctx: Context) {
        val list = getAll(ctx)
        _mailsFlow.value = list
        _unreadFlow.value = list.count { !it.read }
    }

    fun getAll(ctx: Context): List<MoodMail> {
        val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MoodMail(
                    id = o.getString("id"),
                    type = o.getString("type"),
                    emoji = o.getString("emoji"),
                    title = o.getString("title"),
                    body = o.getString("body"),
                    accentHex = o.getLong("accentHex"),
                    createdAt = o.getLong("createdAt"),
                    accepted = o.optBoolean("accepted", false),
                    read = o.optBoolean("read", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAll(ctx: Context, list: List<MoodMail>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("type", m.type); put("emoji", m.emoji)
                put("title", m.title); put("body", m.body); put("accentHex", m.accentHex)
                put("createdAt", m.createdAt); put("accepted", m.accepted); put("read", m.read)
            })
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_LIST, arr.toString()).apply()
    }

    fun push(ctx: Context, mail: MoodMail) {
        val list = getAll(ctx).toMutableList()
        list.add(0, mail)
        // 上限 50 封，超出删除最旧的
        if (list.size > 50) {
            val keep = list.take(50)
            saveAll(ctx, keep)
        } else saveAll(ctx, list)
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_PUSH + mail.type, mail.createdAt).apply()
        refresh(ctx)
    }

    fun update(ctx: Context, id: String, transform: (MoodMail) -> MoodMail) {
        val list = getAll(ctx).map { if (it.id == id) transform(it) else it }
        saveAll(ctx, list)
        refresh(ctx)
    }

    fun lastPushAt(ctx: Context, type: String): Long =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_LAST_PUSH + type, 0L)
}
