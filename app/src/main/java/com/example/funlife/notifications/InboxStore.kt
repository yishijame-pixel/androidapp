// ═════════════════════════════════════════════════════════════════════════
// InboxStore.kt
// 应用内"系统通知收件箱"持久化（多用户隔离，最近 100 条）
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.notifications

import android.content.Context
import com.example.funlife.utils.UserSessionManager
import org.json.JSONArray
import org.json.JSONObject

data class InboxEntry(
    val id: Long,
    val channelId: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val deepLink: String?,
    val read: Boolean
)

object InboxStore {

    private const val FILE = "fun_inbox"
    private const val K_LIST = "list"
    private const val MAX_ENTRIES = 100

    // ── 响应式未读计数：UI 用 collectAsState 即可自动随消息变化更新 ──
    private val _unreadFlow = kotlinx.coroutines.flow.MutableStateFlow(0)
    val unreadFlow: kotlinx.coroutines.flow.StateFlow<Int> = _unreadFlow

    /** UI 重新可见时调用一次（如 ON_RESUME），刷新最新未读数 */
    fun refreshUnread(ctx: Context) {
        _unreadFlow.value = unreadCount(ctx)
    }

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun uidTag(ctx: Context): String {
        val uid = runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
        return if (uid > 0L) uid.toString() else "guest"
    }

    private fun key(ctx: Context): String = "${K_LIST}_${uidTag(ctx)}"

    /** 追加一条通知。返回新建条目的 id。 */
    fun add(
        ctx: Context,
        channel: FunChannel,
        title: String,
        body: String,
        deepLink: String? = null
    ): Long = synchronized(this) {
        try {
            val k = key(ctx)
            val arr = readArray(ctx, k)
            val id = System.currentTimeMillis()
            val entry = JSONObject().apply {
                put("id", id)
                put("ch", channel.id)
                put("title", title)
                put("body", body)
                put("ts", id)
                put("link", deepLink ?: "")
                put("read", false)
            }
            // 倒序：新条目在前
            val newArr = JSONArray()
            newArr.put(entry)
            for (i in 0 until arr.length()) {
                if (newArr.length() >= MAX_ENTRIES) break
                newArr.put(arr.getJSONObject(i))
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
            refreshUnread(ctx)
            id
        } catch (_: Throwable) { 0L }
    }

    fun getAll(ctx: Context): List<InboxEntry> = try {
        val arr = readArray(ctx, key(ctx))
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                InboxEntry(
                    id = o.optLong("id"),
                    channelId = o.optString("ch"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    timestamp = o.optLong("ts"),
                    deepLink = o.optString("link").ifBlank { null },
                    read = o.optBoolean("read", false)
                )
            }.getOrNull()
        }
    } catch (_: Throwable) { emptyList() }

    fun unreadCount(ctx: Context): Int = getAll(ctx).count { !it.read }

    fun markRead(ctx: Context, id: Long) = synchronized(this) {
        mutate(ctx) { o -> if (o.optLong("id") == id) o.put("read", true) }
    }

    fun markAllRead(ctx: Context) = synchronized(this) {
        mutate(ctx) { o -> o.put("read", true) }
    }

    fun delete(ctx: Context, id: Long) = synchronized(this) {
        try {
            val k = key(ctx)
            val arr = readArray(ctx, k)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optLong("id") != id) newArr.put(o)
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
            refreshUnread(ctx)
        } catch (_: Throwable) {}
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().putString(key(ctx), "[]").apply()
        refreshUnread(ctx)
    }

    private fun mutate(ctx: Context, transform: (JSONObject) -> Unit) {
        try {
            val k = key(ctx)
            val arr = readArray(ctx, k)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                transform(o)
                newArr.put(o)
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
            refreshUnread(ctx)
        } catch (_: Throwable) {}
    }

    private fun readArray(ctx: Context, k: String): JSONArray {
        val raw = prefs(ctx).getString(k, "[]") ?: "[]"
        return runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    }
}
