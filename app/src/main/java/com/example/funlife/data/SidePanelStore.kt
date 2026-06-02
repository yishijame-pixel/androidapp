// ═════════════════════════════════════════════════════════════════════════
// SidePanelStore.kt
// 侧边面板数据持久化（用户隔离 SharedPreferences）
//   - 随手记 QuickNote
//   - 星标收藏 StarredItem
//   - 时光胶囊 / 漂流瓶 缓存
// 严格遵循 DEVELOPMENT_PRINCIPLES：多用户隔离 + 防崩防御 + 不抛异常
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.data

import android.content.Context
import com.example.funlife.utils.UserSessionManager
import org.json.JSONArray
import org.json.JSONObject

data class QuickNote(
    val id: Long,
    val text: String,
    val createdAt: Long
)

data class StarredItem(
    val id: Long,
    val type: String,        // "anniversary"/"goal"/"countdown"/"mood"/"note"
    val title: String,
    val subtitle: String,
    val emoji: String,
    val deepLink: String?,
    val starredAt: Long
)

object SidePanelStore {

    private const val FILE = "fun_side_panel"
    private const val K_NOTES = "notes"
    private const val K_STARS = "stars"
    private const val K_BOTTLES_SEEN = "bottles_seen"
    private const val K_LAYOUT = "layout_mode"
    private const val MAX_NOTES = 50
    private const val MAX_STARS = 100

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun uidTag(ctx: Context): String {
        val uid = runCatching { UserSessionManager(ctx).getCurrentUserId() }.getOrDefault(0L)
        return if (uid > 0L) uid.toString() else "guest"
    }

    private fun key(ctx: Context, base: String) = "${base}_${uidTag(ctx)}"

    // ── 随手记 ──
    fun getNotes(ctx: Context): List<QuickNote> = try {
        val arr = JSONArray(prefs(ctx).getString(key(ctx, K_NOTES), "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                QuickNote(o.optLong("id"), o.optString("text"), o.optLong("ts"))
            }.getOrNull()
        }
    } catch (_: Throwable) { emptyList() }

    fun addNote(ctx: Context, text: String): QuickNote? {
        if (text.isBlank()) return null
        return try {
            val k = key(ctx, K_NOTES)
            val arr = JSONArray(prefs(ctx).getString(k, "[]") ?: "[]")
            val now = System.currentTimeMillis()
            val o = JSONObject().put("id", now).put("text", text.trim()).put("ts", now)
            val newArr = JSONArray().put(o)
            for (i in 0 until arr.length()) {
                if (newArr.length() >= MAX_NOTES) break
                newArr.put(arr.getJSONObject(i))
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
            QuickNote(now, text.trim(), now)
        } catch (_: Throwable) { null }
    }

    fun deleteNote(ctx: Context, id: Long) = mutate(ctx, K_NOTES) { it.optLong("id") != id }

    // ── 星标收藏 ──
    fun getStars(ctx: Context): List<StarredItem> = try {
        val arr = JSONArray(prefs(ctx).getString(key(ctx, K_STARS), "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                StarredItem(
                    id = o.optLong("id"),
                    type = o.optString("type"),
                    title = o.optString("title"),
                    subtitle = o.optString("sub"),
                    emoji = o.optString("emoji"),
                    deepLink = o.optString("link").ifBlank { null },
                    starredAt = o.optLong("ts")
                )
            }.getOrNull()
        }
    } catch (_: Throwable) { emptyList() }

    fun addStar(
        ctx: Context, type: String, title: String, subtitle: String,
        emoji: String, deepLink: String?
    ): Long {
        return try {
            val k = key(ctx, K_STARS)
            val arr = JSONArray(prefs(ctx).getString(k, "[]") ?: "[]")
            val now = System.currentTimeMillis()
            val o = JSONObject()
                .put("id", now).put("type", type).put("title", title)
                .put("sub", subtitle).put("emoji", emoji)
                .put("link", deepLink ?: "").put("ts", now)
            val newArr = JSONArray().put(o)
            for (i in 0 until arr.length()) {
                if (newArr.length() >= MAX_STARS) break
                newArr.put(arr.getJSONObject(i))
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
            now
        } catch (_: Throwable) { 0L }
    }

    fun deleteStar(ctx: Context, id: Long) = mutate(ctx, K_STARS) { it.optLong("id") != id }

    // ── 漂流瓶：已读 id 集合（避免重复） ──
    fun markBottleSeen(ctx: Context, key2: String) {
        val k = key(ctx, K_BOTTLES_SEEN)
        val raw = prefs(ctx).getString(k, "") ?: ""
        if (key2 in raw.split(",")) return
        val merged = if (raw.isBlank()) key2 else "$raw,$key2"
        prefs(ctx).edit().putString(k, merged).apply()
    }

    fun isBottleSeen(ctx: Context, key2: String): Boolean {
        val k = key(ctx, K_BOTTLES_SEEN)
        val raw = prefs(ctx).getString(k, "") ?: ""
        return key2 in raw.split(",")
    }

    // ── 布局模式：glass / wall ──
    fun getLayoutMode(ctx: Context): String =
        prefs(ctx).getString(key(ctx, K_LAYOUT), "wall") ?: "wall"

    fun setLayoutMode(ctx: Context, mode: String) {
        prefs(ctx).edit().putString(key(ctx, K_LAYOUT), mode).apply()
    }

    private fun mutate(ctx: Context, base: String, keep: (JSONObject) -> Boolean) {
        try {
            val k = key(ctx, base)
            val arr = JSONArray(prefs(ctx).getString(k, "[]") ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (keep(o)) newArr.put(o)
            }
            prefs(ctx).edit().putString(k, newArr.toString()).apply()
        } catch (_: Throwable) {}
    }
}
