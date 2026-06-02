// ═══════════════════════════════════════════════════════════════════════════
// MoodIconStore.kt
// 心情图标自定义存储：每个用户独立维护一组 MoodIcon（emoji 或上传的图片）
// 内置 14 个 emoji 作为默认值；用户可新增、编辑、删除（仅自定义可删）
// 持久化：SharedPreferences + JSON
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 一个心情图标。
 *
 * @param id 唯一键。内置项 id = emoji 字符本身（"🥰"），保证与历史 MoodEntry.mood 字段兼容；
 *           用户自定义项 id = "custom_<uuid>"。
 * @param value 显示值。emoji 字符 或 本地图片绝对路径。
 * @param label 标签（"超开心"/"平静"…）
 * @param level 心情等级 1..5
 * @param color ARGB 主色（Long）
 * @param isCustom 是否用户自定义（决定能否删除、是否走图片渲染）
 */
data class MoodIcon(
    val id: String,
    val value: String,
    val label: String,
    val level: Int,
    val color: Long,
    val isCustom: Boolean
) {
    /** 是否用图片渲染（value 是文件路径） */
    val isImage: Boolean get() = isCustom && (value.startsWith("/") || value.startsWith("file://"))
}

object MoodIconStore {
    private const val PREF = "mood_icon_store"
    private fun keyFor(userId: Long) = "icons_user_$userId"

    /** 内置 14 个心情（与原 MoodPalette 完全一致，保证老数据 emoji 仍能匹配到 meta） */
    private val DEFAULTS: List<MoodIcon> = listOf(
        MoodIcon("🥰", "🥰", "超开心", 5, 0xFFFF6F91, false),
        MoodIcon("😊", "😊", "开心",   5, 0xFFFF8F4F, false),
        MoodIcon("😃", "😃", "兴奋",   5, 0xFFFFA726, false),
        MoodIcon("🤗", "🤗", "温暖",   4, 0xFFFFB74D, false),
        MoodIcon("😎", "😎", "自信",   4, 0xFF7E57C2, false),
        MoodIcon("😌", "😌", "平静",   3, 0xFF26A69A, false),
        MoodIcon("🤔", "🤔", "思考",   3, 0xFFAB7B3F, false),
        MoodIcon("😶", "😶", "无语",   3, 0xFF9E9E9E, false),
        MoodIcon("😴", "😴", "困倦",   2, 0xFF9575CD, false),
        MoodIcon("🥱", "🥱", "疲惫",   2, 0xFF7986CB, false),
        MoodIcon("😢", "😢", "难过",   1, 0xFF42A5F5, false),
        MoodIcon("😭", "😭", "伤心",   1, 0xFF1E88E5, false),
        MoodIcon("😡", "😡", "生气",   1, 0xFFEF5350, false),
        MoodIcon("😰", "😰", "焦虑",   2, 0xFF607D8B, false),
    )

    /** 默认列表（无副本上传），用于首次读取或重置。 */
    fun defaults(): List<MoodIcon> = DEFAULTS

    /** 读取该用户的全部心情图标。第一次读取时返回默认 14 个（不写盘，写盘发生在用户修改时）。 */
    fun getAll(context: Context, userId: Long): List<MoodIcon> {
        if (userId <= 0L) return DEFAULTS
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = sp.getString(keyFor(userId), null) ?: return DEFAULTS
        return runCatching { decode(json) }.getOrDefault(DEFAULTS)
    }

    /** 替换整组（覆盖式写入）。 */
    fun saveAll(context: Context, userId: Long, list: List<MoodIcon>) {
        if (userId <= 0L) return
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString(keyFor(userId), encode(list)).apply()
    }

    /** 新增一个自定义图标。返回新建的 MoodIcon。 */
    fun addCustom(
        context: Context,
        userId: Long,
        imagePath: String,
        label: String,
        level: Int,
        color: Long
    ): MoodIcon {
        val newIcon = MoodIcon(
            id = "custom_${UUID.randomUUID().toString().take(12)}",
            value = imagePath,
            label = label.ifBlank { "心情" },
            level = level.coerceIn(1, 5),
            color = color,
            isCustom = true
        )
        val cur = getAll(context, userId).toMutableList()
        cur.add(newIcon)
        saveAll(context, userId, cur)
        return newIcon
    }

    /** 更新一个图标（按 id 匹配）。内置项也可改 label/level/color（但 value 不变）。 */
    fun update(
        context: Context,
        userId: Long,
        id: String,
        label: String? = null,
        level: Int? = null,
        color: Long? = null,
        imagePath: String? = null
    ) {
        val cur = getAll(context, userId).toMutableList()
        val idx = cur.indexOfFirst { it.id == id }
        if (idx < 0) return
        val old = cur[idx]
        cur[idx] = old.copy(
            label = label ?: old.label,
            level = level?.coerceIn(1, 5) ?: old.level,
            color = color ?: old.color,
            // 只允许 isCustom 的图标替换 value（图片路径）
            value = if (old.isCustom && imagePath != null) imagePath else old.value
        )
        saveAll(context, userId, cur)
    }

    /** 删除一个自定义图标（内置项不可删）。返回是否成功 & 被删图片路径（供调用方清理文件）。 */
    fun deleteCustom(context: Context, userId: Long, id: String): String? {
        val cur = getAll(context, userId).toMutableList()
        val target = cur.firstOrNull { it.id == id && it.isCustom } ?: return null
        cur.removeAll { it.id == id }
        saveAll(context, userId, cur)
        return target.value
    }

    /** 重置到默认（同时调用方应清理 mood_icons 目录下该用户的所有图片文件）。 */
    fun resetToDefaults(context: Context, userId: Long) {
        if (userId <= 0L) return
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().remove(keyFor(userId)).apply()
    }

    /** 根据 id 或老 emoji 字符查找。找不到返回 null。 */
    fun findById(context: Context, userId: Long, idOrEmoji: String): MoodIcon? {
        return getAll(context, userId).firstOrNull { it.id == idOrEmoji }
    }

    // ─────────────── JSON 编解码 ───────────────
    private fun encode(list: List<MoodIcon>): String {
        val arr = JSONArray()
        list.forEach { ic ->
            arr.put(
                JSONObject().apply {
                    put("id", ic.id)
                    put("value", ic.value)
                    put("label", ic.label)
                    put("level", ic.level)
                    put("color", ic.color)
                    put("isCustom", ic.isCustom)
                }
            )
        }
        return arr.toString()
    }

    private fun decode(json: String): List<MoodIcon> {
        val arr = JSONArray(json)
        val list = mutableListOf<MoodIcon>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                MoodIcon(
                    id = o.getString("id"),
                    value = o.getString("value"),
                    label = o.optString("label", "心情"),
                    level = o.optInt("level", 3),
                    color = o.optLong("color", 0xFFFF8F4FL),
                    isCustom = o.optBoolean("isCustom", false)
                )
            )
        }
        return list
    }
}
